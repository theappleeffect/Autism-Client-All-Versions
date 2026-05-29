plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = property("mod.id") != "template"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        // 1.21.11 renamed ResourceLocation -> Identifier and reorganized several
        // packages. Reverse those for every older version.
        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
            replace("net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer", "net.minecraft.world.entity.vehicle.AbstractMinecartContainer")
            replace("net.minecraft.world.entity.vehicle.boat.ChestBoat", "net.minecraft.world.entity.vehicle.ChestBoat")
            replace("net.minecraft.world.entity.animal.equine.AbstractHorse", "net.minecraft.world.entity.animal.horse.AbstractHorse")
            replace("net.minecraft.client.model.player.PlayerModel", "net.minecraft.client.model.PlayerModel")
            replace("org.jspecify.annotations.Nullable", "org.jetbrains.annotations.Nullable")
            replace("net.minecraft.util.Util", "net.minecraft.Util")
            replace("dimension().identifier()", "dimension().location()")
            replace(".screen.init(window.getGuiScaledWidth(), window.getGuiScaledHeight())", ".screen.init(net.minecraft.client.Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight())")
        }
        // The net.minecraft.client.input.* event objects were introduced in 1.21.9.
        // On older versions, swap them for the mod's compat stand-ins so all internal
        // input plumbing compiles unchanged (mixin injects get primitive signatures
        // via //? conditionals separately).
        string(current.parsed < "1.21.9") {
            replace("net.minecraft.client.input.CharacterEvent", "autismclient.compat.CharacterEvent")
            replace("net.minecraft.client.input.KeyEvent", "autismclient.compat.KeyEvent")
            replace("net.minecraft.client.input.MouseButtonEvent", "autismclient.compat.MouseButtonEvent")
            replace("net.minecraft.client.input.MouseButtonInfo", "autismclient.compat.MouseButtonInfo")
            // authlib GameProfile was record-style (name()/id()/properties()) from 1.21.9;
            // older versions use getName()/getId()/getProperties().
            replace(".getProfile().name()", ".getProfile().getName()")
            replace(".getCurrentProfile().name()", ".getCurrentProfile().getName()")
            replace(".getCurrentProfile().id()", ".getCurrentProfile().getId()")
            replace("profile.id()", "profile.getId()")
            replace("profile.name()", "profile.getName()")
            replace("profile.properties()", "profile.getProperties()")
            replace("texturedProfile.properties()", "texturedProfile.getProperties()")
            // misc renames
            replace(".getWindow().handle()", ".getWindow().getWindow()")
            replace("new ChatScreen(fullCmd, false)", "new ChatScreen(fullCmd)")
            // Input methods used primitive params before 1.21.9. Rewrite the mod's
            // overrides/handlers to primitive signatures (rebuilding a compat event),
            // and convert super.* calls back to primitives.
            replace("public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {", "public boolean mouseClicked(double pmx, double pmy, int pbtn) { autismclient.compat.MouseButtonEvent event = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0)); boolean doubleClick = false;")
            replace("super.mouseClicked(event, doubleClick)", "super.mouseClicked(event.x(), event.y(), event.button())")
            replace("super.mouseClicked(virtualEvent, doubleClick)", "super.mouseClicked(virtualEvent.x(), virtualEvent.y(), virtualEvent.button())")
            replace("public boolean charTyped(CharacterEvent input) {", "public boolean charTyped(char pchr, int pmods) { autismclient.compat.CharacterEvent input = new autismclient.compat.CharacterEvent(pchr, pmods);")
            replace("super.charTyped(input)", "super.charTyped((char) input.codepoint(), input.modifiers())")
            // remaining override signatures.
            // NOTE: PackUtilChatField is a WRAPPER (not a Screen subclass): it keeps its
            // event-style overloads (mouseClicked(MouseButtonEvent click, boolean ignored),
            // keyPressed(KeyEvent keyInput), charTyped(CharacterEvent charInput)) which on
            // <1.21.9 take compat events via the import swaps above. Those overloads delegate
            // to its own primitive mouseClicked(double,double,int). Do NOT rewrite them here
            // (they use unique param names ignored/keyInput/charInput) or they collide with
            // the primitive overload and break callers that pass compat events.
            replace("public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {", "public boolean mouseClicked(double pmx, double pmy, int pbtn) { autismclient.compat.MouseButtonEvent click = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0)); boolean doubled = false;")
            replace("public boolean mouseReleased(MouseButtonEvent click) {", "public boolean mouseReleased(double pmx, double pmy, int pbtn) { autismclient.compat.MouseButtonEvent click = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0));")
            replace("public boolean mouseReleased(MouseButtonEvent event) {", "public boolean mouseReleased(double pmx, double pmy, int pbtn) { autismclient.compat.MouseButtonEvent event = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0));")
            replace("public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {", "public boolean mouseDragged(double pmx, double pmy, int pbtn, double deltaX, double deltaY) { autismclient.compat.MouseButtonEvent click = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0));")
            replace("public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {", "public boolean mouseDragged(double pmx, double pmy, int pbtn, double dx, double dy) { autismclient.compat.MouseButtonEvent event = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0));")
            // genuine @Override charTyped in the book/sign mixins (param name "input", distinct
            // from the wrapper's "charInput"): rewrite to the primitive vanilla signature.
            replace("public boolean charTyped(CharacterEvent input) {", "public boolean charTyped(char pchr, int pmods) { autismclient.compat.CharacterEvent input = new autismclient.compat.CharacterEvent(pchr, pmods);")
            // remaining super.* calls
            replace("super.mouseClicked(click, doubled)", "super.mouseClicked(click.x(), click.y(), click.button())")
            replace("super.mouseReleased(click)", "super.mouseReleased(click.x(), click.y(), click.button())")
            replace("super.mouseReleased(virtualEvent(event))", "super.mouseReleased(virtualEvent(event).x(), virtualEvent(event).y(), virtualEvent(event).button())")
            replace("super.mouseDragged(click, deltaX, deltaY)", "super.mouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY)")
            replace("super.mouseDragged(virtualEvent, PackUtilUiScale.toVirtual(dx), PackUtilUiScale.toVirtual(dy))", "super.mouseDragged(virtualEvent.x(), virtualEvent.y(), virtualEvent.button(), PackUtilUiScale.toVirtual(dx), PackUtilUiScale.toVirtual(dy))")
            // mixin @Inject handler signatures (primitive targets pre-1.21.9)
            replace("private void yang\$keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {", "private void yang\$keyPressed(int pk, int ps, int pm, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.KeyEvent input = new autismclient.compat.KeyEvent(pk, ps, pm);")
            replace("private void packutil\$keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {", "private void packutil\$keyPressed(int pk, int ps, int pm, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.KeyEvent input = new autismclient.compat.KeyEvent(pk, ps, pm);")
            replace("private void yang\$keyReleased(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {", "private void yang\$keyReleased(int pk, int ps, int pm, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.KeyEvent input = new autismclient.compat.KeyEvent(pk, ps, pm);")
            replace("private void yang\$mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {", "private void yang\$mouseClicked(double pmx, double pmy, int pbtn, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.MouseButtonEvent click = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0)); boolean doubled = false;")
            replace("private void yang\$mouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {", "private void yang\$mouseReleased(double pmx, double pmy, int pbtn, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.MouseButtonEvent click = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0));")
            replace("private void yang\$mouseDragged(MouseButtonEvent click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {", "private void yang\$mouseDragged(double pmx, double pmy, int pbtn, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.MouseButtonEvent click = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0));")
            replace("private void yang\$charTyped(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {", "private void yang\$charTyped(char pchr, int pmods, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.CharacterEvent input = new autismclient.compat.CharacterEvent(pchr, pmods);")
            replace("private void packutil\$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {", "private void packutil\$mouseClicked(double pmx, double pmy, int pbtn, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.MouseButtonEvent event = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0)); boolean doubleClick = false;")
            replace("private void packutil\$mouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {", "private void packutil\$mouseReleased(double pmx, double pmy, int pbtn, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.MouseButtonEvent event = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0));")
            replace("private void packutil\$mouseDragged(MouseButtonEvent event, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {", "private void packutil\$mouseDragged(double pmx, double pmy, int pbtn, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.MouseButtonEvent event = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0));")
            replace("private void packutil\$handleOverlayClick(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {", "private void packutil\$handleOverlayClick(double pmx, double pmy, int pbtn, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.MouseButtonEvent click = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0)); boolean doubled = false;")
            replace("private void packutil\$handleOverlayDrag(MouseButtonEvent click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {", "private void packutil\$handleOverlayDrag(double pmx, double pmy, int pbtn, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.MouseButtonEvent click = new autismclient.compat.MouseButtonEvent(pmx, pmy, new autismclient.compat.MouseButtonInfo(pbtn, 0));")
            replace("private void packutil\$handleOverlayKeys(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {", "private void packutil\$handleOverlayKeys(int pk, int ps, int pm, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.KeyEvent input = new autismclient.compat.KeyEvent(pk, ps, pm);")
            replace("private void packutil\$handleOverlayChars(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {", "private void packutil\$handleOverlayChars(char pchr, int pmods, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.CharacterEvent input = new autismclient.compat.CharacterEvent(pchr, pmods);")
            replace("private void packutil\$charTyped(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {", "private void packutil\$charTyped(char pchr, int pmods, CallbackInfoReturnable<Boolean> cir) { autismclient.compat.CharacterEvent input = new autismclient.compat.CharacterEvent(pchr, pmods);")

            // ---- subsystem API back-ports (<1.21.9) ----
            // PlayerSkin moved to net.minecraft.world.entity.player in 1.21.9; older = client.resources.
            replace("net.minecraft.world.entity.player.PlayerSkin", "net.minecraft.client.resources.PlayerSkin")
            // SkinManager.get(...) was getOrLoad(...) before 1.21.9.
            replace("getSkinManager()::get", "getSkinManager()::getOrLoad")
            replace("getSkinManager().get(profile)", "getSkinManager().getOrLoad(profile)")
            // Minecraft.services() bundle (1.21.9+) -> individual accessors / older equivalents.
            replace(".services().sessionService()", ".getMinecraftSessionService()")
            replace("this.minecraft.services().profileResolver().fetchById(id).orElse(new GameProfile(id, name))", "java.util.Optional.ofNullable(this.minecraft.getMinecraftSessionService().fetchProfile(id, true)).map(com.mojang.authlib.yggdrasil.ProfileResult::profile).orElse(new GameProfile(id, name))")
            replace("this.minecraft.services().profileResolver().fetchByName(username)", "java.util.Optional.<com.mojang.authlib.GameProfile>empty()")
            replace("resolved.get().properties()", "resolved.get().getProperties()")
            // SkinManager ctor changed; SkinTextureDownloader is absent before 1.21.9.
            replace("new SkinManager(skinCachePath, services, new SkinTextureDownloader(mc.getProxy(), mc.getTextureManager(), mc), mc)", "new SkinManager(skinCachePath, services.sessionService(), net.minecraft.Util.backgroundExecutor())")
            // ClientboundAddEntityPacket.getMovement() (1.21.9+) -> velocity components.
            replace("addEntity.getMovement()", "new net.minecraft.world.phys.Vec3(addEntity.getXa(), addEntity.getYa(), addEntity.getZa())")
            // KeyMapping.matches(KeyEvent) (1.21.9+) -> matches(key, scancode).
            replace("keyInventory.matches(input)", "keyInventory.matches(input.key(), input.scancode())")
            replace("binding.matches(input)", "binding.matches(input.key(), input.scancode())")
            // User had a trailing User.Type before 1.21.9 removed it.
            replace("new User(username, UUIDUtil.createOfflinePlayerUUID(username), \"\", Optional.empty(), Optional.empty())", "new User(username, UUIDUtil.createOfflinePlayerUUID(username), \"\", Optional.empty(), Optional.empty(), net.minecraft.client.User.Type.LEGACY)")
            replace("new User(username, UndashedUuid.fromStringLenient(uuid), token, Optional.empty(), Optional.empty())", "new User(username, UndashedUuid.fromStringLenient(uuid), token, Optional.empty(), Optional.empty(), net.minecraft.client.User.Type.MSA)")
            replace("auth.getAccessToken(), Optional.empty(), Optional.empty())", "auth.getAccessToken(), Optional.empty(), Optional.empty(), net.minecraft.client.User.Type.MSA)")
            // authlib Environment was 3-arg (sessionHost, servicesHost, name) before 7.x.
            replace("new Environment(\"http://sessionserver.thealtening.com\", \"http://authserver.thealtening.com\", \"https://api.mojang.com\", \"The Altening\")", "new Environment(\"http://sessionserver.thealtening.com\", \"http://authserver.thealtening.com\", \"The Altening\")")
            // Screen/Gui render-rewrite helpers (1.21.9+).
            replace("this.packutil\$minecraft.screen.renderWithTooltipAndSubtitles(graphics, 0, 0, a)", "this.packutil\$minecraft.screen.render(graphics, 0, 0, a)")
            replace("this.packutil\$minecraft.screen.renderWithTooltipAndSubtitles(graphics, mouseX, mouseY, a)", "this.packutil\$minecraft.screen.render(graphics, mouseX, mouseY, a)")
            // NOTE: do NOT add replace() rules whose REPLACEMENT value is a common token
            // (";", "true", etc). Stonecutter reverses replacements when leaving a version,
            // and a common replacement value corrupts every occurrence on reverse.
            // renderDeferredSubtitles + panoramaShouldSpin are handled in-source instead.
            // PlayerSkinRenderCache (1.21.9+) import: its usage block is //? gated; drop the import too.
            replace("import net.minecraft.client.renderer.PlayerSkinRenderCache;", "// PlayerSkinRenderCache (1.21.9+) unavailable")
            // PlayerSkin.body().texturePath() (1.21.9+) -> texture() (ResourceLocation) on older.
            replace("skin.body().texturePath()", "skin.texture()")
        }
        // 1.21.5 rewrote CompoundTag getters: Optional-returning getters + getXxxOr(key, default).
        // Route those call sites through the PackUtilNbt shim on older versions (the shim itself is
        // excluded from 1.21.5+ compilation in build.gradle.kts). Receivers enumerated from source.
        string(current.parsed < "1.21.5") {
            replace("tag.getStringOr(", "autismclient.util.PackUtilNbt.getStringOr(tag, ")
            replace("tag.getBooleanOr(", "autismclient.util.PackUtilNbt.getBooleanOr(tag, ")
            replace("tag.getIntOr(", "autismclient.util.PackUtilNbt.getIntOr(tag, ")
            replace("tag.getLongOr(", "autismclient.util.PackUtilNbt.getLongOr(tag, ")
            replace("tag.getFloatOr(", "autismclient.util.PackUtilNbt.getFloatOr(tag, ")
            replace("tag.getDoubleOr(", "autismclient.util.PackUtilNbt.getDoubleOr(tag, ")
            replace("tag.getList(", "autismclient.util.PackUtilNbt.getList(tag, ")
            replace("workingTag.getStringOr(", "autismclient.util.PackUtilNbt.getStringOr(workingTag, ")
            replace("workingTag.getBooleanOr(", "autismclient.util.PackUtilNbt.getBooleanOr(workingTag, ")
            replace("workingTag.getIntOr(", "autismclient.util.PackUtilNbt.getIntOr(workingTag, ")
            replace("workingTag.getDoubleOr(", "autismclient.util.PackUtilNbt.getDoubleOr(workingTag, ")
            replace("workingTag.getList(", "autismclient.util.PackUtilNbt.getList(workingTag, ")
            replace("rootTag.getStringOr(", "autismclient.util.PackUtilNbt.getStringOr(rootTag, ")
            replace("rootTag.getIntOr(", "autismclient.util.PackUtilNbt.getIntOr(rootTag, ")
            replace("rootTag.getList(", "autismclient.util.PackUtilNbt.getList(rootTag, ")
            replace("mapTag.getIntOr(", "autismclient.util.PackUtilNbt.getIntOr(mapTag, ")
            replace("entryTag.getStringOr(", "autismclient.util.PackUtilNbt.getStringOr(entryTag, ")
            replace("entryTag.getIntOr(", "autismclient.util.PackUtilNbt.getIntOr(entryTag, ")
            replace("actionTag.getStringOr(", "autismclient.util.PackUtilNbt.getStringOr(actionTag, ")
            // Optional-returning getCompound(key)/getString(key) (1.21.5+) -> shim Optionals.
            replace("tag.getCompound(", "autismclient.util.PackUtilNbt.getCompound(tag, ")
            replace("workingTag.getCompound(", "autismclient.util.PackUtilNbt.getCompound(workingTag, ")
            replace("rootTag.getCompound(", "autismclient.util.PackUtilNbt.getCompound(rootTag, ")
            replace("mapTag.getCompound(", "autismclient.util.PackUtilNbt.getCompound(mapTag, ")
            replace("entryTag.getCompound(", "autismclient.util.PackUtilNbt.getCompound(entryTag, ")
            replace("tag.getString(", "autismclient.util.PackUtilNbt.getString(tag, ")
            replace("workingTag.getString(", "autismclient.util.PackUtilNbt.getString(workingTag, ")
            replace("rootTag.getString(", "autismclient.util.PackUtilNbt.getString(rootTag, ")
            replace("mapTag.getString(", "autismclient.util.PackUtilNbt.getString(mapTag, ")
            replace("entryTag.getString(", "autismclient.util.PackUtilNbt.getString(entryTag, ")
            replace("actionTag.getString(", "autismclient.util.PackUtilNbt.getString(actionTag, ")
            replace("c2sList.getString(", "autismclient.util.PackUtilNbt.getString(c2sList, ")
            replace("s2cList.getString(", "autismclient.util.PackUtilNbt.getString(s2cList, ")
            replace("list.getString(", "autismclient.util.PackUtilNbt.getString(list, ")
            // NBT tail: short/byte convenience, OrEmpty list, Optional-returning array getters.
            replace("tag.getShortOr(", "autismclient.util.PackUtilNbt.getShortOr(tag, ")
            replace("tag.getByteOr(", "autismclient.util.PackUtilNbt.getByteOr(tag, ")
            replace("tag.getListOrEmpty(", "autismclient.util.PackUtilNbt.getListOrEmpty(tag, ")
            replace("tag.getByteArray(", "autismclient.util.PackUtilNbt.getByteArray(tag, ")
            replace("tag.getIntArray(", "autismclient.util.PackUtilNbt.getIntArray(tag, ")
            replace("tag.getLongArray(", "autismclient.util.PackUtilNbt.getLongArray(tag, ")
            // CompoundTag.keySet() (1.21.5+) -> getAllKeys(); TagParser.parseCompoundFully -> parseTag.
            replace("tag.keySet()", "tag.getAllKeys()")
            replace("net.minecraft.nbt.TagParser.parseCompoundFully(", "net.minecraft.nbt.TagParser.parseTag(")
            // Inventory.getSelectedSlot()/setSelectedSlot() (1.21.5+) -> selected field / setSelectedHotbarSlot.
            replace(".getInventory().getSelectedSlot()", ".getInventory().selected")
            replace(".getInventory().setSelectedSlot(", ".getInventory().setSelectedHotbarSlot(")
            // ClientboundContainerSetContentPacket record-style accessors (1.21.5+) -> 1.21.4 getters.
            replace("inventory.items()", "inventory.getItems()")
            replace("inventory.carriedItem()", "inventory.getCarriedItem()")
            replace("inventory.stateId()", "inventory.getStateId()")
            replace("inventory.containerId()", "inventory.getContainerId()")
            // Tag.asString() returns Optional<String> in 1.21.5+ -> PackUtilNbt.asString shim (Optional).
            replace("el.asString()", "autismclient.util.PackUtilNbt.asString(el)")
            replace("element.asString()", "autismclient.util.PackUtilNbt.asString(element)")
            replace("nbt.asString()", "autismclient.util.PackUtilNbt.asString(nbt)")
            // ServerboundContainerClickPacket: carried became HashedStack + ctor arg order changed (1.21.5).
            // Route the (simple-form) call sites through PackUtilCompat.click which rebuilds the
            // 1.21.4 packet; PackUtilCompat uses the FQN ctor so this rule won't self-match it.
            replace("new ServerboundContainerClickPacket(", "autismclient.util.PackUtilCompat.click(")
            // Map HashedStack refs to UNIQUE PackUtilCompat members (reverse-safe; a common target
            // like ItemStack.EMPTY/Void.class would corrupt every occurrence on reverse). The FQN
            // HashedStack.EMPTY sites are normalized to the simple form in-source first.
            replace("HashedStack.EMPTY", "autismclient.util.PackUtilCompat.EMPTY")
            replace("HashedStack.class", "autismclient.util.PackUtilCompat.HASHED_CLASS")
            replace("Int2ObjectMap<net.minecraft.network.HashedStack>", "Int2ObjectMap<net.minecraft.world.item.ItemStack>")
            replace("import net.minecraft.network.HashedStack;", "// HashedStack (1.21.5+) unavailable")
        }
        // 1.21.6 render rewrite: GuiGraphics.pose() became a 2D Matrix3x2fStack, RenderPipelines
        // replaced the RenderType getters in blit, and nextStratum/submitSkinRenderState/getPanorama
        // were added. Map those back to the pre-1.21.6 PoseStack / RenderType::guiTextured / helpers.
        string(current.parsed < "1.21.6") {
            // blit's first arg is Function<ResourceLocation,RenderType> pre-1.21.6.
            replace("RenderPipelines.GUI_TEXTURED", "net.minecraft.client.renderer.RenderType::guiTextured")
            replace("import net.minecraft.client.renderer.RenderPipelines;", "// RenderPipelines (1.21.6+) -> RenderType::guiTextured")
            // Connection.send's listener param was PacketSendListener before 1.21.6 (ChannelFutureListener after).
            replace("import io.netty.channel.ChannelFutureListener;", "import net.minecraft.network.PacketSendListener;")
            replace("Lio/netty/channel/ChannelFutureListener;", "Lnet/minecraft/network/PacketSendListener;")
            replace("ChannelFutureListener listener", "PacketSendListener listener")
            // 2D Matrix3x2fStack (1.21.6+) -> 3D PoseStack.
            replace(".pose().pushMatrix()", ".pose().pushPose()")
            replace(".pose().popMatrix()", ".pose().popPose()")
            replace(".pose().translate(centerX, centerY)", ".pose().translate(centerX, centerY, 0.0)")
            replace(".pose().translate(-centerX, -centerY)", ".pose().translate(-centerX, -centerY, 0.0)")
            replace(".pose().scale(scale, scale)", ".pose().scale(scale, scale, 1.0F)")
            // nextStratum / submitSkinRenderState (1.21.6+) -> PackUtilRender no-op helpers.
            // getPanorama() is gated inline in PackUtilTitleScreen via //? if (renders the
            // inherited Screen.renderPanorama on <1.21.6 so the menu paints a full backdrop).
            replace("context.nextStratum()", "autismclient.util.PackUtilRender.nextStratum(context)")
            replace("ctx.nextStratum()", "autismclient.util.PackUtilRender.nextStratum(ctx)")
            replace("graphics.nextStratum()", "autismclient.util.PackUtilRender.nextStratum(graphics)")
            replace("graphics.submitSkinRenderState(", "autismclient.util.PackUtilRender.submitSkin(graphics, ")
        }
    }
}
