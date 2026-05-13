package autismclient.gui.packui;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.UniformType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PackUiTextPipelines {
    static final RenderPipeline UI_TEXT = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("autismclient", "pipeline/ui_text"))
        .withVertexFormat(PackUiVertexFormats.POS2_TEXTURE_COLOR, com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES)
        .withVertexShader(Identifier.fromNamespaceAndPath("autismclient", "text"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("autismclient", "text"))
        .withSampler("u_Texture")
        .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .build();

    private PackUiTextPipelines() {
    }

    public static void precompile(ResourceManager resources) {
        RenderSystem.getDevice().precompilePipeline(UI_TEXT, (identifier, shaderType) -> readShaderSource(resources, identifier, shaderType));
    }

    private static String readShaderSource(ResourceManager resources, Identifier identifier, com.mojang.blaze3d.shaders.ShaderType type) {
        Identifier fileLocation = type.idConverter().idToFile(identifier);
        Resource resource = resources.getResource(fileLocation)
            .orElseThrow(() -> new IllegalStateException("Missing PackUI shader resource: " + fileLocation));

        try (var in = resource.open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PackUI shader source: " + fileLocation, e);
        }
    }
}
