// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.3") // 使用最新版本
    }
}

plugins {
    id("org.sonarqube") version "3.3" // 使用最新版本
}

apply(plugin = "org.sonarqube")

sonarqube {
    properties {
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.sources", "src/main/java") // 如果项目没有子项目，可以直接在这里配置
        property("sonar.login", "admin")
        property("sonar.password", "admin")
    }
}

if (project.hasProperty("subprojects")) {
    subprojects {
        sonarqube {
            properties {
                property("sonar.sources", "src/main/java")
            }
        }
    }
}
