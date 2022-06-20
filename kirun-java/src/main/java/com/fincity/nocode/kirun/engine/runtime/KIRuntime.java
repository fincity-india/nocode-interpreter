package com.fincity.nocode.kirun.engine.runtime;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fincity.nocode.kirun.engine.Repository;
import com.fincity.nocode.kirun.engine.exception.KIRuntimeException;
import com.fincity.nocode.kirun.engine.function.AbstractFunction;
import com.fincity.nocode.kirun.engine.function.Function;
import com.fincity.nocode.kirun.engine.json.schema.Schema;
import com.fincity.nocode.kirun.engine.json.schema.SchemaUtil;
import com.fincity.nocode.kirun.engine.model.Event;
import com.fincity.nocode.kirun.engine.model.EventResult;
import com.fincity.nocode.kirun.engine.model.FunctionDefinition;
import com.fincity.nocode.kirun.engine.model.FunctionSignature;
import com.fincity.nocode.kirun.engine.model.Parameter;
import com.fincity.nocode.kirun.engine.model.ParameterReference;
import com.fincity.nocode.kirun.engine.model.ParameterReference.ParameterReferenceType;
import com.fincity.nocode.kirun.engine.model.Statement;
import com.fincity.nocode.kirun.engine.runtime.expression.Expression;
import com.fincity.nocode.kirun.engine.runtime.expression.ExpressionEvaluator;
import com.fincity.nocode.kirun.engine.runtime.expression.ExpressionToken;
import com.fincity.nocode.kirun.engine.runtime.graph.ExecutionGraph;
import com.fincity.nocode.kirun.engine.runtime.graph.GraphVertex;
import com.fincity.nocode.kirun.engine.util.string.StringFormatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

public class KIRuntime extends AbstractFunction {

	private static final String PARAMETER_NEEDS_A_VALUE = "Parameter \"$\" needs a value";

	private final FunctionDefinition fd;

	private final Repository<Function> fRepo;

	private final Repository<Schema> sRepo;

	private static final int VERSION = 1;

	public KIRuntime(FunctionDefinition fd, Repository<Function> functionRepository,
	        Repository<Schema> schemaRepository) {

		this.fd = fd;
		if (this.fd.getVersion() > VERSION) {
			throw new KIRuntimeException("Runtime is at a lower version " + VERSION
			        + " and trying to run code from version " + this.fd.getVersion() + ".");
		}

		this.fRepo = functionRepository;
		this.sRepo = schemaRepository;
	}

	@Override
	public FunctionSignature getSignature() {

		return this.fd;
	}

	private ExecutionGraph<String, StatementExecution> getExecutionPlan(Map<String, ContextElement> context) {

		ExecutionGraph<String, StatementExecution> g = new ExecutionGraph<>();
		for (Statement s : this.fd.getSteps()
		        .values())
			g.addVertex(this.prepareStatementExecution(context, s));

		var unresolvedList = this.makeEdges(g);

		if (!unresolvedList.isEmpty()) {
			throw new KIRuntimeException(
			        StringFormatter.format("Found these unresolved dependencies : $ ", unresolvedList.stream()
			                .map(e -> StringFormatter.format("Steps.$.$", e.getT1(), e.getT2()))));
		}

		return g;
	}

	@Override
	protected Flux<EventResult> internalExecute(final FunctionExecutionParameters inContext) {

		if (inContext.getContext() == null)
			inContext.setContext(new ConcurrentHashMap<>());

		if (inContext.getEvents() == null)
			inContext.setEvents(new ConcurrentHashMap<>());

		ExecutionGraph<String, StatementExecution> eGraph = this.getExecutionPlan(inContext.getContext());

		boolean hasError = eGraph.getVerticesDataFlux()
		        .flatMap(e -> Flux.fromIterable(e.getMessages()))
		        .map(StatementMessage::getMessageType)
		        .filter(e -> e == StatementMessageType.ERROR)
		        .take(1)
		        .blockFirst() == null;

		if (hasError) {
			throw new KIRuntimeException("Please fix the errors before execution");
		}

		return Mono.fromCallable(() -> executeGraph(eGraph, inContext))
		        .subscribeOn(Schedulers.boundedElastic())
		        .flatMapIterable(e -> e);

	}

	private List<EventResult> executeGraph(ExecutionGraph<String, StatementExecution> eGraph,
	        FunctionExecutionParameters inContext) {

		LinkedList<GraphVertex<String, StatementExecution>> executionQue = new LinkedList<>();
		executionQue.addAll(eGraph.getVerticesWithNoIncomingEdges());

		LinkedList<Tuple4<ExecutionGraph<String, StatementExecution>, List<Tuple2<String, String>>, Flux<EventResult>, GraphVertex<String, StatementExecution>>> branchQue = new LinkedList<>();

		Map<String, Map<String, Map<String, JsonElement>>> output = new ConcurrentHashMap<>();

		while ((!executionQue.isEmpty() || !branchQue.isEmpty()) && !inContext.getEvents()
		        .containsKey(Event.OUTPUT)) {

			processBranchQue(inContext, executionQue, branchQue, output);
			processExecutionQue(inContext, executionQue, branchQue, output);
		}

		if (inContext.getEvents()
		        .isEmpty()) {

			throw new KIRuntimeException("No events raised.");
		}

		return inContext.getEvents()
		        .entrySet()
		        .stream()
		        .flatMap(e -> e.getValue()
		                .stream()
		                .map(v -> EventResult.of(e.getKey(), v)))
		        .toList();
	}

	private void processExecutionQue(FunctionExecutionParameters inContext,
	        LinkedList<GraphVertex<String, StatementExecution>> executionQue,
	        LinkedList<Tuple4<ExecutionGraph<String, StatementExecution>, List<Tuple2<String, String>>, Flux<EventResult>, GraphVertex<String, StatementExecution>>> branchQue,
	        Map<String, Map<String, Map<String, JsonElement>>> output) {

		if (!executionQue.isEmpty()) {

			var vertex = executionQue.pop();

			if (!allDependenciesResolved(vertex, output))

				executionQue.add(vertex);

			else
				executeVertex(vertex, inContext, output, branchQue, executionQue);
		}
	}

	private void processBranchQue(FunctionExecutionParameters inContext,
	        LinkedList<GraphVertex<String, StatementExecution>> executionQue,
	        LinkedList<Tuple4<ExecutionGraph<String, StatementExecution>, List<Tuple2<String, String>>, Flux<EventResult>, GraphVertex<String, StatementExecution>>> branchQue,
	        Map<String, Map<String, Map<String, JsonElement>>> output) {
		if (!branchQue.isEmpty()) {

			var branch = branchQue.pop();

			if (!allDependenciesResolved(branch.getT2(), output))
				branchQue.add(branch);
			else
				executeBranch(inContext, executionQue, output, branch);

		}
	}

	private void executeBranch(FunctionExecutionParameters inContext,
	        LinkedList<GraphVertex<String, StatementExecution>> executionQue,
	        Map<String, Map<String, Map<String, JsonElement>>> output,
	        Tuple4<ExecutionGraph<String, StatementExecution>, List<Tuple2<String, String>>, Flux<EventResult>, GraphVertex<String, StatementExecution>> branch) {

		var vertex = branch.getT4();
		EventResult nextOutput = null;

		do {
			this.executeGraph(branch.getT1(), inContext);
			nextOutput = branch.getT3()
			        .next()
			        .block();
			if (nextOutput != null)
				output.computeIfAbsent(vertex.getData()
				        .getStatement()
				        .getStatementName(), k -> new ConcurrentHashMap<>())
				        .put(nextOutput.getName(), nextOutput.getResult());
		} while (nextOutput != null && !nextOutput.getName()
		        .equals(Event.OUTPUT));

		if (nextOutput != null && nextOutput.getName()
		        .equals(Event.OUTPUT)) {

			vertex.getOutVertices()
			        .get(Event.OUTPUT)
			        .stream()
			        .forEach(executionQue::add);
		}
	}

	private void executeVertex(GraphVertex<String, StatementExecution> vertex, FunctionExecutionParameters inContext,
	        Map<String, Map<String, Map<String, JsonElement>>> output,
	        LinkedList<Tuple4<ExecutionGraph<String, StatementExecution>, List<Tuple2<String, String>>, Flux<EventResult>, GraphVertex<String, StatementExecution>>> branchQue,
	        LinkedList<GraphVertex<String, StatementExecution>> executionQue) {

		Statement s = vertex.getData()
		        .getStatement();

		Function fun = this.fRepo.find(s.getNamespace(), s.getName());

		Map<String, Parameter> paramSet = fun.getSignature()
		        .getParameters();

		Map<String, JsonElement> arguments = getArgumentsFromParametersMap(inContext, output, s, paramSet);

		Map<String, ContextElement> context = inContext.getContext();

		Flux<EventResult> result = fun.execute(new FunctionExecutionParameters().setContext(context)
		        .setArguments(arguments)
		        .setEvents(inContext.getEvents())
		        .setStatementExecution(vertex.getData()));

		EventResult er = result.next()
		        .block();

		if (er == null)
			throw new KIRuntimeException(
			        StringFormatter.format("Executing $ returned no events", s.getStatementName()));

		boolean isOutput = er.getName()
		        .equals(Event.OUTPUT);

		output.computeIfAbsent(s.getStatementName(), k -> new ConcurrentHashMap<>())
		        .put(er.getName(), er.getResult());

		if (!isOutput) {

			var subGraph = vertex.getSubGraphOfType(er.getName());
			List<Tuple2<String, String>> unResolvedDependencies = this.makeEdges(subGraph);
			branchQue.add(Tuples.of(subGraph, unResolvedDependencies, result, vertex));
		} else {

			vertex.getOutVertices()
			        .get(Event.OUTPUT)
			        .stream()
			        .forEach(executionQue::add);
		}
	}

	private boolean allDependenciesResolved(List<Tuple2<String, String>> unResolvedDependencies,
	        Map<String, Map<String, Map<String, JsonElement>>> output) {

		return unResolvedDependencies.stream()
		        .takeWhile(e -> output.containsKey(e.getT1()) && output.get(e.getT1())
		                .containsKey(e.getT2()))
		        .count() == unResolvedDependencies.size();
	}

	private boolean allDependenciesResolved(GraphVertex<String, StatementExecution> vertex,
	        Map<String, Map<String, Map<String, JsonElement>>> output) {

		if (vertex.getInVertices()
		        .isEmpty())
			return true;

		return vertex.getInVertices()
		        .stream()
		        .filter(e ->
			        {

				        String stepName = e.getT1()
				                .getData()
				                .getStatement()
				                .getName();
				        String type = e.getT2();

				        return !(output.containsKey(stepName) && output.get(stepName)
				                .containsKey(type));
			        })
		        .count() == 0;
	}

	private Map<String, JsonElement> getArgumentsFromParametersMap(final FunctionExecutionParameters inContext,
	        Map<String, Map<String, Map<String, JsonElement>>> output, Statement s, Map<String, Parameter> paramSet) {

		return s.getParameterMap()
		        .entrySet()
		        .stream()
		        .map(e ->
			        {
				        List<ParameterReference> prList = e.getValue();

				        JsonElement ret = null;

				        if (prList == null || prList.isEmpty())
					        return Tuples.of(e.getKey(), ret);

				        Parameter pDef = paramSet.get(e.getKey());

				        if (pDef.isVariableArgument()) {

					        ret = new JsonArray();

					        prList.stream()
					                .map(r -> this.parameterReferenceEvaluation(inContext, output, r))
					                .flatMap(r -> r.isJsonArray() ? StreamSupport.stream(r.getAsJsonArray()
					                        .spliterator(), false) : Stream.of(r))
					                .forEachOrdered(((JsonArray) ret)::add);

				        } else {

					        ret = this.parameterReferenceEvaluation(inContext, output, prList.get(0));
				        }

				        return Tuples.of(e.getKey(), ret);
			        })
		        .filter(e -> !(e.getT2() == null || e.getT2()
		                .isJsonNull()))
		        .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2));
	}

	private JsonElement parameterReferenceEvaluation(final FunctionExecutionParameters inContext,
	        Map<String, Map<String, Map<String, JsonElement>>> output, ParameterReference ref) {

		JsonElement ret = null;

		if (ref.getType() == ParameterReferenceType.VALUE) {
			ret = ref.getValue();
		} else if (ref.getType() == ParameterReferenceType.EXPRESSION && ref.getExpression() != null
		        && !ref.getExpression()
		                .isBlank()) {
			ExpressionEvaluator exp = new ExpressionEvaluator(ref.getExpression());
			ret = exp.evaluate(inContext, output);
		}
		return ret;
	}

	private StatementExecution prepareStatementExecution(Map<String, ContextElement> context, Statement s) { // NOSONAR
	                                                                                                         // -
	                                                                                                         // Breaking
	                                                                                                         // this
	                                                                                                         // logic
	                                                                                                         // will be
	                                                                                                         // meaning
	                                                                                                         // less

		StatementExecution se = new StatementExecution(s);

		Function fun = this.fRepo.find(s.getNamespace(), s.getName());

		HashMap<String, Parameter> paramSet = new HashMap<>(fun.getSignature()
		        .getParameters());

		for (Entry<String, List<ParameterReference>> param : s.getParameterMap()
		        .entrySet()) {

			Parameter p = paramSet.get(param.getKey());

			List<ParameterReference> refList = param.getValue();

			if (refList == null || refList.isEmpty()) {

				if (SchemaUtil.getDefaultValue(p.getSchema(), this.sRepo) == null)
					se.addMessage(StatementMessageType.ERROR,
					        StringFormatter.format(PARAMETER_NEEDS_A_VALUE, p.getParameterName()));
				continue;
			}

			if (p.isVariableArgument()) {

				for (ParameterReference ref : refList)
					parameterReferenceValidation(context, se, p, ref);
			} else {

				ParameterReference ref = refList.get(0);
				parameterReferenceValidation(context, se, p, ref);
			}

			paramSet.remove(p.getParameterName());
		}

		if (!paramSet.isEmpty()) {
			for (Parameter param : paramSet.values()) {
				if (SchemaUtil.getDefaultValue(param.getSchema(), this.sRepo) == null)
					se.addMessage(StatementMessageType.ERROR,
					        StringFormatter.format(PARAMETER_NEEDS_A_VALUE, param.getParameterName()));
			}
		}

		return se;
	}

	private void parameterReferenceValidation(Map<String, ContextElement> context, StatementExecution se, Parameter p, // NOSONAR
	                                                                                                                   // -
	                                                                                                                   // Breaking
	                                                                                                                   // this
	                                                                                                                   // logic
	                                                                                                                   // will
	                                                                                                                   // be
	                                                                                                                   // meaning
	                                                                                                                   // less
	        ParameterReference ref) {

		if (ref == null) {
			if (SchemaUtil.getDefaultValue(p.getSchema(), this.sRepo) == null)
				se.addMessage(StatementMessageType.ERROR,
				        StringFormatter.format(PARAMETER_NEEDS_A_VALUE, p.getParameterName()));
		} else if (ref.getType() == ParameterReferenceType.VALUE) {
			if (ref.getValue() == null && SchemaUtil.getDefaultValue(p.getSchema(), this.sRepo) == null)
				se.addMessage(StatementMessageType.ERROR,
				        StringFormatter.format(PARAMETER_NEEDS_A_VALUE, p.getParameterName()));
		} else if (ref.getType() == ParameterReferenceType.EXPRESSION) {
			if (ref.getExpression() == null || ref.getExpression()
			        .isBlank()) {
				if (SchemaUtil.getDefaultValue(p.getSchema(), this.sRepo) == null)
					se.addMessage(StatementMessageType.ERROR,
					        StringFormatter.format(PARAMETER_NEEDS_A_VALUE, p.getParameterName()));
			} else {
				try {
					Expression exp = new Expression(ref.getExpression());
					this.typeCheckExpression(context, p, exp);
					this.addDependencies(se, exp);
				} catch (KIRuntimeException ex) {
					se.addMessage(StatementMessageType.ERROR,
					        StringFormatter.format("Error evaluating $ : ", ref.getExpression(), ex.getMessage()));
				}
			}
		}
	}

	private void addDependencies(StatementExecution se, Expression exp) {

		LinkedList<Expression> que = new LinkedList<>();
		que.add(exp);

		while (!que.isEmpty()) {
			for (ExpressionToken token : que.getFirst()
			        .getTokens()) {
				if (token instanceof Expression e) {
					que.push(e);
				} else if (token.getExpression()
				        .startsWith("Steps.")) {
					se.addDependency(token.getExpression());
				}
			}
		}

		if (se.getStatement()
		        .getDependentStatements() == null)
			return;

		for (String statement : se.getStatement()
		        .getDependentStatements())
			se.addDependency(statement);
	}

	private void typeCheckExpression(Map<String, ContextElement> context, Parameter p, Expression exp) {

		// TODO: we need to check the type of the parameters based on the input they
		// get.
	}

	public List<Tuple2<String, String>> makeEdges(ExecutionGraph<String, StatementExecution> graph) {

		return graph.getNodeMap()
		        .values()
		        .stream()
		        .filter(e -> e.getData()
		                .getDepenedencies() != null)
		        .flatMap(e -> e.getData()
		                .getDepenedencies()
		                .stream()
		                .map(d ->
			                {
				                int secondDot = d.indexOf('.', 6);
				                String step = d.substring(6, secondDot);
				                String event = d.substring(secondDot + 1, d.indexOf('.', secondDot + 1));

				                if (!graph.getNodeMap()
				                        .containsKey(step))
					                return Tuples.of(step, event);

				                e.addInEdgeTo(graph.getNodeMap()
				                        .get(step), event);
				                return null;
			                })
		                .filter(Objects::nonNull))
		        .toList();

	}
}
