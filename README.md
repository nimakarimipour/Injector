# Injector
A tool to inject annotations to source codes written in Java

# Usage

An annotation can be injected into three locations:
1. ```Method Parameter```
2. ```Method Return```
3. ```Class Field```

Creat e ```Fix``` object like below:

```java
Fix fix = new  Fix(
        annotation, //e.g. javax.annotation.Nullable
        method, //e.g. run(java.lang.Object)
        param, //e.g. either class field or method parameter
        location, // (METHOD_PARAM || METHOD_RETURN || CLASS_FIELD)
        className, // com.org.Run
        uri, // uri to file
        inject // true to inject, false to remove
        );
```

And then pass it the ```injector```, and call ```start```. The injector will add/remove multiple annotations in one pass
of parsing the file.

```java
Injector injector = Injector.builder().setMode(Injector.MODE.BATCH).build();
List<Fix> fixes;
injector.start(fixes);
```

```Injector``` preserves the code style of the source code and will not remove any white spaces after removal.

## Dependencies

```Injector``` is written using [javaparser](https://javaparser.org) framework and the dependency is handled by the build system.
