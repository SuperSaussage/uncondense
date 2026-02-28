plugins { 
    java 
}

group = "com.example"
version = "1.0.0"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.devs.beer/") // 👈 ItemsAdder repo
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    // 👇 ItemsAdder API (compile only)
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.3-beta-14")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
