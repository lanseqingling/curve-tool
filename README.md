# Curve-Tool

## 介绍

### 概述

Curve-Tool 是一个简易的数据曲线计算工具，使用函数式编程的方式实现自定义公式计算、多曲线叠加计算、分组计算等，一些场景可提高开发效率，简化代码，使开发者专注核心计算而非维护数据关系。

![](https://fastly.jsdelivr.net/gh/chocohQL/ql-file@main/assets/github/curve2.png)

### 使用场景

+ 轻量级数据可视化，不使用 Prometheus & Grafana 等专业工具。
+ 数据曲线的处理计算或不同类型数据曲线的叠加计算。
+ 关注核心计算，避免大量维护数据关系（如遍历、分组、条件）。

![](https://fastly.jsdelivr.net/gh/chocohQL/ql-file@main/assets/githubQLC-tool-02.png)

### 工具缺陷

+ 多曲线计算需要严格对齐数据，无法应对复杂处理场景。
+ 大数据量计算效率较低，后续考虑使用 Fork/Join 框架优化。
+ 大量函数式编程会造成更多调试分析的障碍。

## 快速开始

### 创建曲线

ICurve 接口定义了曲线计算方法，它继承了 List 。Curve 类是该接口的通用实现类，它本身是对 ArrayList 的增强。

```java
@Data
public class Data {
    private Double val;
    private Long timestamp;
}
```

```java
// 获取数据集
List<Data> data = getData();
// 创建曲线，ICurve<T, V> 泛型1表示数据本身，泛型2表示用于计算的属性类型
ICurve<Data, Double> curve2 = new Curve<>(data);
```

### 单曲线处理

process() 方法针对的是曲线每一个元素，第一个表达式产生结果，第二个表达式对第一个表达式产生的结果进行消费，这样设计实际上是分离了处理和消费的逻辑，因此你可以传递 setter 方法引用对结果赋值，当然也可以自定义其他消费逻辑。

```java
// 集合中每个数据的 val 属性 x2 后赋值 
curve.process(d -> d.getVal() * 2, Data1::setVal);
// 集合中每个数据的 val 属性 x2 后打印
curve.process(d -> d.getVal() * 2, (d, v) -> System.out.println(v));
```

三个参数的 process() 方法为需要传递条件断言，满足条件的才会执行后续定义的计算逻辑

```java
// 集合中数据的 val 属性满足 > 0.5 这个条件的 x2 后赋值
curve.process(d -> d.getVal() > 0.5, d -> d.getVal() * 2, Data1::setVal)
```

如果你不想要拆分条件、处理和消费逻辑，那么你也可以直接使用单一参数的 process() 方法，类似于 peek() 方法

```java
curve.process(d -> {
        // TODO ...
        d.setVal(d.getVal() * 2);
        // TODO ...
    });
```

### 多曲线叠加处理

![](https://fastly.jsdelivr.net/gh/chocohQL/ql-file@main/assets/githubQLC-tool-03.svg)

如果你需要对两条曲线进行叠加处理，并且它们的在曲线集合中的数据一一对应（长度相同、下标对应），那么可以使用 biProcess() 方法，你可以传递不同类型的曲线集合，只需要保证泛型2类型一致（用于计算）。

```java
@Data
public class Data1 {
    private Double val;
    private Long timestamp;
}

@Data
public class Data2 {
    private Double val;
    private Long timestamp;
}
```

```java
// 获取数据集
ArrayList<Data1> data1 = getData1List();
ArrayList<Data2> data2 = getData2List();
// 创建曲线
ICurve<Data2, Double> curve2 = new Curve<>(data2);
ICurve<Data1, Double> curve1 = new Curve<>(data1);

curve1
        // 叠加计算
        .biProcess(curve2, (d1, d2) -> d1.getVal() + d2.getVal(), Data1::setVal)
        // 条件叠加计算
        .biProcess(curve2,
                (d1, d2) -> 1 <= d1.getVal() * d2.getVal(),
                (d1, d2) -> d1.getVal() + d2.getVal(),
                Data1::setVal)
```

### 创建分组曲线

如果你需要将数据分为多条曲线（分组/维度），且需要对不同组的曲线进行独立的处理，那么可以使用分组曲线，它可以帮你划分数据集隔离执行相同的 ICurve 操作。

![](https://fastly.jsdelivr.net/gh/chocohQL/ql-file@main/assets/githubQLC-tool-06.svg)

ICurveGroup 接口继承了 Map ，它有三个泛型，第一个为分组 Key 的类型，后两个与 Curve 一致，CurveGroup 为通用实现类，提供了创建分组曲线的方法，需要传入数据集合和进行分组的规则。需要注意的是分组并不是指一个 key 对应了多条曲线，分组是针对的数据，它实际上是 Map<K, ICurve<T, V>> 的形式，在两个 CurveGroup 进行叠加计算时的分组才是多条曲线。

```java
@Data
public class Data {
    private Double val;
    private Long timestamp;
    private String tag;
}
```

```java
ArrayList<Data> data = data1();
// 根据 Data 的 tag 属性进行分组
ICurveGroup<String, Data, Double> curveGroup = CurveGroup.create(data, Data::getTag);
```

### 分组曲线处理

ICurveGroup 的处理逻辑和 ICurve 类似，它会在表达式中多提供一个 key 辅助你的处理，如果是两个分组曲线进行计算，会自动匹配相同分组的两条曲线，于是你可以将他当成普通的 ICurve 处理。

```java
@Data
public class Data1 {
    private Double val;
    private Long timestamp;
    private String tag;
}

@Data
public class Data2 {
    private Double val;
    private Long timestamp;
    private String tag;
}
```

```java
curveGroup1
        // 分组计算
        .process((tag, d) -> d.getVal() + 0.1, Data1::setVal)
        // 分组条件计算
        .process((tag, d) -> d.getVal() > 0.5, (tag, d) -> d.getVal() + 0.1, Data1::setVal)
        // 分组叠加计算
        .biProcess(curveGroup2, (tag, d1, d2) -> d1.getVal() + d2.getVal(), Data1::setVal)
        // 分组条件叠加计算
        .biProcess(curveGroup2,
                (tag, d1, d2) -> tag.equals("tagA") || d1.getVal() + d2.getVal() > 0.5,
                (tag, d1, d2) -> d1.getVal() * 2,
                Data1::setVal)
```

有时候你也许并不想直接对具体数据进行操作，而是想操作不同分组下的曲线，那么你也可以使用 forCurve 方法调出对应分组的曲线来自定义处理过程。

```java
curveGroup1.forCurve((key, curve1) -> {/*TODO*/})

curveGroup1.forCurve(curveGroup2, (tag, curve1, curve2) -> {
            curve1.process(d ->{/*TODO*/});
            curve2.process(d ->{/*TODO*/});
        });
```

## 关系图

![](https://fastly.jsdelivr.net/gh/chocohQL/ql-file@main/assets/githubQLC-tool-08.svg)
