import { Expression } from '../../../../src/engine/runtime/expression/Expression';

test('Expression Test', () => {
    expect(new Expression('2+3').toString()).toBe('(2+3)');
    expect(new Expression('2.234 + 3 * 1.22243').toString()).toBe('((2.234)+(3*(1.22243)))');
    expect(new Expression('10*11+12*13*14/7').toString()).toBe('((10*11)+(12*(13*(14/7))))');
    expect(new Expression('34 << 2 = 8 ').toString()).toBe('((34<<2)=8)');

    let ex: Expression = new Expression(
        'Context.a[Steps.loop.iteration.index - 1]+ Context.a[Steps.loop.iteration.index - 2]',
    );

    expect(ex.toString()).toBe(
        '((Context.(a[((Steps.(loop.(iteration.index)))-1)))+(Context.(a[((Steps.(loop.(iteration.index)))-2))))',
    );

    ex = new Expression('Steps.step1.output.obj.array[Steps.step1.output.obj.num +1]+2');
    expect(ex.toString()).toBe(
        '((Steps.(step1.(output.(obj.(array[((Steps.(step1.(output.(obj.num))))+1))))))+2)',
    );

    let arrays: Expression = new Expression(
        'Context.a[Steps.loop.iteration.index][Steps.loop.iteration.index + 1]',
    );
    let deepObject: Expression = new Expression('Context.a.b.c');
    let deepObjectWithArray: Expression = new Expression('Context.a.b[2].c');

    expect(arrays.toString()).toBe(
        '(Context.(a[((Steps.(loop.(iteration.index)))[((Steps.(loop.(iteration.index)))+1))))',
    );
    expect(deepObject.toString()).toBe('(Context.(a.(b.c)))');
    expect(deepObjectWithArray.toString()).toBe('(Context.(a.(b[(2.c))))');

    let opInTheName = new Expression('Store.a.b.c or Store.c.d.x');
    expect(opInTheName.toString()).toBe('((Store.(a.(b.c)))or(Store.(c.(d.x))))');

    opInTheName = new Expression('Store.a.b.corStore.c.d.x');
    expect(opInTheName.toString()).toBe('(Store.(a.(b.(corStore.(c.(d.x))))))');
});
