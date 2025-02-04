import { Schema, SchemaType, SchemaValidator } from '../../../../../src';

test('NotValidation', async () => {
    let sch = Schema.of(
        'Not Schema',
        SchemaType.INTEGER,
        SchemaType.LONG,
        SchemaType.FLOAT,
        SchemaType.DOUBLE,
        SchemaType.STRING,
    )
        .setDefaultValue(1)
        .setNot(Schema.of('Not String', SchemaType.STRING));

    const value = await SchemaValidator.validate(undefined, sch, undefined, 0);
    console.log(value);
    expect(value).toBe(0);

    sch = Schema.of(
        'Not Schema',
        SchemaType.INTEGER,
        SchemaType.LONG,
        SchemaType.FLOAT,
        SchemaType.DOUBLE,
        SchemaType.STRING,
    )
        .setDefaultValue(1)
        .setNot(Schema.of('Not Integer', SchemaType.INTEGER));

    await expect(SchemaValidator.validate(undefined, sch, undefined, 0)).rejects.toThrow();

    sch = Schema.of('Not Schema', SchemaType.INTEGER, SchemaType.LONG, SchemaType.FLOAT)
        .setDefaultValue(1)
        .setNot(new Schema().setConstant(0));

    await expect(SchemaValidator.validate(undefined, sch, undefined, 0)).rejects.toThrow();
    expect(await SchemaValidator.validate(undefined, sch, undefined, null)).toBe(1);
    expect(await SchemaValidator.validate(undefined, sch, undefined, 2)).toBe(2);
});

test('constantValidation', async () => {
    let sch = Schema.of('Constant Schema', SchemaType.INTEGER).setConstant(1);

    const value = await SchemaValidator.validate(undefined, sch, undefined, 1);
    expect(value).toBe(1);

    await expect(SchemaValidator.validate(undefined, sch, undefined, 0)).rejects.toThrow();
});
