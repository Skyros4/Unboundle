plugins {
    id("dev.kikugie.loom-back-compat")
    id("dev.kikugie.stonecutter")
}

// If one .jar supports multiple versions (e.g. 1.21.9 and 1.21.10), add the range to the filename
val compatibleVersions = sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }
val releaseSuffix = if (compatibleVersions.size > 1) "${compatibleVersions.first()}-${compatibleVersions.last()}" else sc.current.version

// DO NOT set group = ...!
version = "${property("mod.version")}+$releaseSuffix"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang Mappings on obfuscated versions
    if (sc.current.parsed < "26.1") {
        loomx.applyMojangMappings()
    }

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    fapi("fabric-lifecycle-events-v1", "fabric-resource-loader-v0", "fabric-content-registries-v0", "fabric-registry-sync-v0", "fabric-rendering-v1")
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth_config")}")
    modCompileOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}")
    modLocalRuntime("me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth_config")}")
    modLocalRuntime("com.terraformersmc:modmenu:${property("deps.modmenu")}")
}

loom {
    splitEnvironmentSourceSets()
    mods {
        register("unboundle") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run") // Shares the run directory between versions
        jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
    }
}

sourceSets {
    listOf("main", "client").forEach { name ->
        named(name) {
            java {
                if (sc.current.parsed >= "26.1") {
                    srcDir("src/$name/java-26.1+")
                } else {
                    srcDir("src/$name/java-1.21.x")
                }
            }
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val minecraftRange = if (compatibleVersions.size <= 1) {
            sc.properties["mod.mc_compat"]
        } else {
            ">=${compatibleVersions.first()} <=${compatibleVersions.last()}+"
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            inputs.property("minecraft", minecraftRange)
            set("minecraft", minecraftRange)
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"

        // loomx.mod(Sources)Jar returns the jar task for the applied loom variant
        from(loomx.modJar.map { it.archiveFile }, loomx.modSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }

    jar {
        inputs.property("archivesName", base.archivesName)
        from("LICENSE") {
            rename { "${it}_${base.archivesName.get()}" }
        }
    }
}