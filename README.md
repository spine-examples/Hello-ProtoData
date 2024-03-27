# Hello-ProtoData
An example on code generation with ProtoData.

This example demonstrates how to define a new Protobuf option and
generate the validation code in response to this option
using ProtoData API.

### Custom Protobuf Option

Below is example on Protobuf message definition 
with custom `size` option that is used for validating 
the size of a repeated field.

```protobuf
// A board for TicTacToe game.
//
// A board may have any size but minimum 3-by-3.
// The field `side_size` defines the size of the board's side and
// the number of cells is checked with the `size` option so that
// this number always equals the "side_size * side_size" value.
//
message Board {

  // The name of the board.
  BoardName name = 1 [(required) = true, (validate) = true];

  // Board cells.
  repeated Cell cell = 2 [(required) = true, (size).value = "side_size * side_size"];

  // The size of the board.
  //
  // The board must have `side_size` number of cell rows,
  // each having `side_size` number of columns,
  // effectively making the board "square".
  //
  int32 side_size = 3 [(required) = true, (min).value = "3"];
}

message BoardName {
  string value = 1 [(required) = true];
}

enum Mark {
  M_UNDEFINED = 0;
  CROSS = 1;
  CIRCLE = 2;
}

message Cell {
  oneof value {
    bool empty = 1;
    Mark mark = 2;
  }
}
```
See the `model` subproject for details.

### How to Define a Custom Option

Below is a definition of Protobuf extension that can be found in `options.proto`:
```protobuf
extend google.protobuf.FieldOptions {

    // See `ArrayOfSizeOption` for details.
    //
    // The field index is chosen based in `spine/options.proto`,
    // taking the next available number in Spine's reserved range.
    //
    ArrayOfSizeOption size = 73855;
}

// A field option applicable to `repeated` fields,
// telling that their size should be equal
// to some expression, involving the values
// of other fields of the same message.
//
// The `value` field supports basic math operations,
// such as `+`, `-`, `*`, `/`.
//
// Example:
//
// message Foo {
//
//     int32 count = 1;
//
//     // There must be a number of elements
//     // twice the `count`.
//     repeated string value = 2 [(size),value = "count * 2"];
// }
message ArrayOfSizeOption {

    string value = 1 [(required) = true];
}
```
Also, there is `ArrayOfSizeOptionProvider` that registers this extension:
```kotlin
/**
 * Registers Protobuf extension that enables `ArrayOfSizeOption` field option
 * that may be applied to a repeated field in order to validate its size.
 */
@AutoService(OptionsProvider::class)
public class ArrayOfSizeOptionProvider : OptionsProvider {

    override fun registerIn(registry: ExtensionRegistry) {
        ArrayOfSizeOptionProto.registerAllExtensions(registry)
    }
}
```
Please note that options provider should be marked with `@AutoService` annotation.

See the `proto-extension` subproject for details.

### Code Generation

There is `ApplySizeOptionPlugin` that implemented in order to generate 
the validation code for the `size` option.
The plugin can be deployed during the build process 
and uses ProtoData API to extend the code generation for Protobuf messages.

The plugin generates extensions for message builder classes 
with validation methods for every field with `size` option applied.

Also, the `build()` method of a message builder class is extended 
to call these methods.

See the `codegen-plugin` subproject for details.

### Plugin Deployment

The following configuration should be applied in the Gradle configuration 
of a subproject to use the `size` option:
```kotlin
protoData {
    // Run ProtoData with the `size` option plugin enabled.
    plugins(
        "io.spine.protodata.hello.ApplySizeOptionPlugin"
    )
}
```
Also, the dependencies on `proto-extension` and `codegen-plugin`
subprojects should be added:
```kotlin
dependencies {
    // Enable field options extension.
    api(project(":proto-extension"))
    // Add module with code generation plugin to ProtoData classpath.
    protoData(project(":codegen-plugin"))
}
```
### Integration Tests

The negative test-cases are implemented in a specific way.

Such tests configure and run the build of a test project
in a separate Gradle process because every negative case raises
the appropriate error and fails the build.

A negative test-case is accepted when the specific error message 
is found in the stderr stream of the failed build process.

See the `integration-tests` subproject for details.
