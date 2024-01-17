# Developers


## Building from source



Clone the ClintLR project from GitHub, then checkout and package.

```
$ git clone https://github.com/TheJacksonLaboratory/ClintLR.git
$ cd ClintLR
$ ./mvnw --also-make --batch-mode package -P release
```


To complete setup, download the LIRICAL resources and compile.

```
$ java -jar lirical-cli/target/lirical-cli-2.0.0.jar download
$ ./mvnw clean compile
```


