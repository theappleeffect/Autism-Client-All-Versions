plugins {
    // Applies the correct loom variant based on the Minecraft version.
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    else -> JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang Mappings on obfuscated versions.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    // Full Fabric API: this mod touches many modules, so pull the lot.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")

    // External libraries the published source uses but never declared.
    // implementation = compile classpath; include = bundled (JiJ) so they exist at RUNTIME.
    // Without include, these classes are absent in the shipped jar -> NoClassDefFoundError
    // (waybackauthlib on the Accounts screen; netty socks/proxy on the Proxies screen).
    // isTransitive = false: authlib + gson (waybackauthlib's deps) and netty-handler/codec/transport
    // (the proxy libs' deps) are already provided by Minecraft at runtime.
    implementation("de.florianreuth:waybackauthlib:1.1.0")
    include("de.florianreuth:waybackauthlib:1.1.0") { isTransitive = false }
    // SOCKS proxy handlers are NOT bundled by Minecraft; pin to MC's netty (deps.netty).
    implementation("io.netty:netty-handler-proxy:${sc.properties.get<String>("deps.netty")}") { isTransitive = false }
    include("io.netty:netty-handler-proxy:${sc.properties.get<String>("deps.netty")}") { isTransitive = false }
    implementation("io.netty:netty-codec-socks:${sc.properties.get<String>("deps.netty")}") { isTransitive = false }
    include("io.netty:netty-codec-socks:${sc.properties.get<String>("deps.netty")}") { isTransitive = false }
}

loom {
    // Useful for interface injection / mod metadata awareness.
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runConfigs.all {
        // Share the run directory between versions.
        runDir = "../../run"
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

// The custom GPU text engine (PackUi*TextMesh*/Texture/VertexFormats) needs the 1.21.6+
// render API (GpuBufferSlice, DynamicUniformStorage, RenderPass, 2D pose stack). On older
// versions PackUiText falls back to vanilla Font, so these leaf classes are excluded from
// compilation; PackUiTextPipelines/Uniforms/AtlasTextRenderer carry //? if >=1.21.6 stubs.
if (sc.current.parsed < "1.21.6") {
    sourceSets.named("main") {
        java.exclude(
            "**/PackUiTextTexture.java",
            "**/PackUiTextMeshRenderer.java",
            "**/PackUiTextMeshBuilder.java",
            "**/PackUiVertexFormats.java"
        )
    }
}

// PackUtilNbt is the pre-1.21.5 CompoundTag getter shim; on 1.21.5+ the old String-returning
// getters were replaced by Optional-returning ones, so the shim only compiles below 1.21.5.
if (sc.current.parsed >= "1.21.5") {
    sourceSets.named("main") {
        java.exclude("**/PackUtilNbt.java")
    }
}

// BookSignScreen was split out in 1.21.5; before that the signing UI is part of BookEditScreen,
// so the @Mixin(BookSignScreen.class) target doesn't exist. Exclude the mixin below 1.21.5.
if (sc.current.parsed < "1.21.5") {
    sourceSets.named("main") {
        java.exclude("**/PackUtilBookSignScreenMixin.java")
    }
}

tasks {
    processResources {
        val javaMixin = "JAVA_${requiredJava.majorVersion}"
        val javaMin = requiredJava.majorVersion

        val modVersion = sc.properties.get<String>("mod.version")
        val mcCompat = sc.properties.get<String>("mod.mc_compat")
        val dropBookSignMixin = sc.current.parsed < "1.21.5"

        inputs.property("javaMixin", javaMixin)
        inputs.property("javaMin", javaMin)
        inputs.property("modVersion", modVersion)
        inputs.property("mcCompat", mcCompat)
        inputs.property("dropBookSignMixin", dropBookSignMixin)

        filesMatching("fabric.mod.json") {
            expand(
                "version" to modVersion,
                "minecraft" to mcCompat,
                "java" to javaMin
            )
        }
        filesMatching("*.mixins.json") {
            expand("java" to javaMixin)
            // PackUtilBookSignScreenMixin is excluded from compilation below 1.21.5 (its target
            // class doesn't exist yet); drop its config entry so the mixin processor doesn't fail
            // trying to load a class that isn't in the jar.
            if (dropBookSignMixin) {
                filter { line -> if (line.contains("PackUtilBookSignScreenMixin")) "" else line }
            }
        }
    }

    // Builds the version into a shared folder: build/libs/${mod.version}/
    register<Copy>("buildAndCollect") {
        group = "build"
        from(loomx.modJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}
