[![Build Status][ubuntu-build-badge]][gh-actions]
[![Build Status][windows-build-badge]][gh-actions]
[![license][license-badge]][apache-license]

# Introduction

Declarations in a `.proto` file can be annotated with 
a number of [options][proto3-options].
Options do not change the overall meaning of a declaration, 
but may affect the way it is handled in a particular context.

Protobuf supports different types of options, e.g. file-level options, 
message-level options, field-level options, etc. The list of 
available options types is defined in 
[/google/protobuf/descriptor.proto][descriptor-proto].

Protobuf also allows to define and use [custom options][proto3-custom-options]. 
The authors say "that this is an advanced feature which most people don’t need" 
and it actually requires a significant amount of work to define a custom option 
and generate the validation code for this option. However, ProtoData provides 
a fairly easy way to collect the custom options metadata and extend the 
generated code with the desired behaviour.

# Domain

This use-case of applying ProtoData demonstrates how to enrich the model 
with additional semantic elements and reflect their meaning in the generated 
code.

For example, let's define a board for Tic-tac-toe game:

```protobuf
// A board for Tic-tac-toe game.
message Board {
  // The name of the board.
  BoardName name = 1;

  // The size of the board.
  int32 side_size = 2;
  
  // Board cells. It is expected to have "side_size * side_size" cells. 
  repeated Cell cell = 3;
}

message BoardName {
  string value = 1;
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
Definitely, we can use this model but it is so easy to make some mistake when 
the model becomes invalid state. What can we do to avoid this?

It would be much better if we could add the following:

1. Mark some fields as `required` to state the absolute necessity to fill them 
with real values.
2. Set the minimum acceptable value for the `side_size` field.
3. Add some validation rules that verify the size of the `cell` collection.
Indeed, a tic-tac-toe board should be square, and hence have an appropriate 
number of cells, being the size of the side squared.

We can easily solve the points 1 and 2 as [Spine][spine-web-site] provides 
options to mark the fields as `required` and validate values of the numeric 
fields.

Let's add these elements to the `Board` definition:

```protobuf
// A board for Tic-tac-toe game.
message Board {
  // The name of the board.
  BoardName name = 1 [(required) = true];

  // The size of the board.
  int32 side_size = 2 [(required) = true, (min).value = "3"];

  // Board cells. The collection must not be empty.
  repeated Cell cell = 3 [(required) = true];
}
...
```

To solve the point 3, we can define a custom option so we could set the 
required size for a repeated field and use ProtoData API to extend the generated 
by Protobuf code with the additional logic that checks the instances of Proto 
messages for validness at runtime. So let's do it.

Below is definition of the `size` custom option:
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
message ArrayOfSizeOption {

    string value = 1 [(required) = true];
}
```

Now let's use this option in the `Board` definition:

```protobuf
// A board for Tic-tac-toe game.
message Board {
  // The name of the board.
  BoardName name = 1 [(required) = true];

  // The size of the board.
  //
  // The board must have `side_size` number of cell rows,
  // each having `side_size` number of columns,
  // effectively making the board "square".
  //
  int32 side_size = 2 [(required) = true, (min).value = "3"];

  // Board cells. It is required to have "side_size * side_size" cells.
  repeated Cell cell = 3 [(required) = true, (size).value = "side_size * side_size"];
}
...
```

The [ApplySizeOptionPlugin][plugin-source] is implemented to generate 
the validation code for the `size` option. This plugin uses ProtoData API
to collect the `size` option metadata and to generate extensions 
for the message builder classes with validation methods for every field 
with `size` option applied.

Below is generated validation method for the `Board.cell` field.

```kotlin
internal fun Board.Builder.validateCellCount(): Board.Builder {
    val expected = sideSize * sideSize
    check(cellCount == expected) {
        "Invalid number of 'cell' elements: " +
            "expected $expected, but actual $cellCount."
    }
    return this
}
```

Also, the `build()` method of a message builder class is updated
to call such methods.

See the [codegen-plugin][plugin-module] subproject for details.

The following configuration should be applied in the Gradle configuration 
of a module to use the `size` option plugin:
```kotlin
protoData {
    // Run ProtoData with the `size` option plugin enabled.
    plugins(
        "io.spine.protodata.hello.ApplySizeOptionPlugin"
    )
}
```

# Development

### Prerequisites

This example is implemented using the following technologies and tools:

1. Java 11
2. Kotlin 1.9.23
3. Gradle 7.6

### Building locally

1. Clone the repository:
```bash
git clone git@github.com:spine-examples/Hello-ProtoData.git
```
2. Run the Gradle build:
```bash
./gradlew build
```

[gh-actions]: https://github.com/spine-examples/Hello-ProtoData/actions
[ubuntu-build-badge]: https://github.com/spine-examples/Hello-ProtoData/actions/workflows/build-on-ubuntu.yml/badge.svg
[windows-build-badge]: https://github.com/spine-examples/Hello-ProtoData/actions/workflows/build-on-windows.yml/badge.svg
[license-badge]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat
[apache-license]: http://www.apache.org/licenses/LICENSE-2.0

[proto3-options]: https://protobuf.dev/programming-guides/proto3/#options
[descriptor-proto]: https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/descriptor.proto
[proto3-custom-options]: https://protobuf.dev/programming-guides/proto3/#customoptions
[plugin-source]: codegen-plugin/src/main/kotlin/io/spine/examples/protodata/hello/plugin/ApplySizeOptionPlugin.kt
[plugin-module]: codegen-plugin
[spine-web-site]: https://spine.io/
