package autismclient.mixin;

import autismclient.util.PackUtilProxy;
import autismclient.util.PackUtilProxyManager;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(value = Connection.class, priority = 2000)
public abstract class PackUtilConnectionProxyMixin {
    @Inject(method = "configureSerialization", at = @At("HEAD"), require = 0)
    private static void packutil$disableMeteorProxyBeforeHandlers(ChannelPipeline pipeline, PacketFlow inboundDirection, boolean local, @Nullable BandwidthDebugMonitor monitor, CallbackInfo ci) {
        if (local || inboundDirection != PacketFlow.CLIENTBOUND) return;
        packutil$disableMeteorProxy();
    }

    @Inject(method = "configureSerialization", at = @At("RETURN"), require = 0)
    private static void packutil$addProxyHandler(ChannelPipeline pipeline, PacketFlow inboundDirection, boolean local, @Nullable BandwidthDebugMonitor monitor, CallbackInfo ci) {
        if (local || inboundDirection != PacketFlow.CLIENTBOUND) return;
        PackUtilProxy proxy = PackUtilProxyManager.get().getEnabled();
        if (proxy == null) return;
        InetSocketAddress address = new InetSocketAddress(proxy.address, proxy.port);
        switch (proxy.type) {
            case Socks4 -> pipeline.addFirst("packutil_socks4_proxy", new Socks4ProxyHandler(address, proxy.username == null ? "" : proxy.username));
            case Socks5 -> pipeline.addFirst("packutil_socks5_proxy", new Socks5ProxyHandler(address, proxy.username == null ? "" : proxy.username, proxy.password == null ? "" : proxy.password));
        }
    }

    private static void packutil$disableMeteorProxy() {
        if (!FabricLoader.getInstance().isModLoaded("meteor-client")) return;
        try {
            Class<?> proxiesClass = Class.forName("meteordevelopment.meteorclient.systems.proxies.Proxies");
            Class<?> proxyClass = Class.forName("meteordevelopment.meteorclient.systems.proxies.Proxy");
            Object proxies = proxiesClass.getMethod("get").invoke(null);
            Object enabled = proxiesClass.getMethod("getEnabled").invoke(proxies);
            if (enabled != null) proxiesClass.getMethod("setEnabled", proxyClass, boolean.class).invoke(proxies, enabled, false);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
