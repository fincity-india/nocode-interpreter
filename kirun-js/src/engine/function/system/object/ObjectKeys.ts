import { FunctionOutput } from '../../../model/FunctionOutput';
import { FunctionExecutionParameters } from '../../../runtime/FunctionExecutionParameters';

import { isNullValue } from '../../../util/NullCheck';
import { EventResult } from '../../../model/EventResult';
import { AbstractObjectFunction } from './AbstractObjectFunction';

const VALUE = 'value';

export class ObjectKeys extends AbstractObjectFunction {
    public constructor() {
        super('ObjectEntries');
    }

    protected async internalExecute(context: FunctionExecutionParameters): Promise<FunctionOutput> {
        var source = context.getArguments()?.get('source');

        if (isNullValue(source))
            return new FunctionOutput([EventResult.outputOf(new Map([[VALUE, []]]))]);

        let keys: String[] = Object.keys(source);

        return new FunctionOutput([EventResult.outputOf(new Map([[VALUE, keys]]))]);
    }
}
