### 前言

请使用JDK8 只能这个大版本，open-jdk也不行

-----
## idea配置

idea 添加这个参数,解决tron-protobuf模块种的Protocol.java文件因为过大编译不成功/报错问题<br/>
方法一:<br/>
Help -> Edit Custom VM Options... 然后，新建一行添加，添加以下内容，然后重启idea即可生效
```config

-Didea.max.intellisense.filesize=999999

```
方法二:<br/>
不引入tron-protobuf项目，而是直接导入libs种的tron-protobuf-xxx.jar包
```bash
cd libs

mvn install:install-file -Dfile=tron-protobuf-1.0-SNAPSHOT.jar -DgroupId=org.tron -DartifactId=tron-protobuf -Dversion=1.0-SNAPSHOT -Dpackaging=jar

```

-----

## 引入${JRE_HOME}/lib/rt.jar

为了方便引用 我直接放到libs下了

```bash
cd libs

mvn install:install-file -Dfile=rt.jar -DgroupId=jdk8 -DartifactId=java-rt -Dversion=1.8 -Dpackaging=jar

```



## 联系方式

telegram: @TrickyZh

QQ: 420713155
