package ru.acid.realdlc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Rounded add-server form with working editable fields. */
public final class RealDlcAddServerScreen extends Screen {
    private EditBox name;
    private EditBox address;
    private String error = "";
    private int age;

    public RealDlcAddServerScreen() { super(Component.literal("Добавить сервер")); }

    @Override
    protected void init() {
        int pw = Math.min(540, width - 36), ph = 304;
        int x = (width-pw)/2, y = (height-ph)/2;
        name = new EditBox(font, x+32, y+72, pw-64, 32, Component.literal("Название"));
        name.setMaxLength(64);
        name.setHint(UiDraw.text("Например, Друзья"));
        name.setBordered(false);
        name.setTextColor(0xFFFFFFFF);
        address = new EditBox(font, x+32, y+140, pw-64, 32, Component.literal("IP-адрес"));
        address.setMaxLength(255);
        address.setHint(UiDraw.text("play.example.net:25565"));
        address.setBordered(false);
        address.setTextColor(0xFFFFFFFF);
        addRenderableWidget(name);
        addRenderableWidget(address);
        setInitialFocus(name);
    }

    @Override public void tick() { age++; super.tick(); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        UiDraw.background(g, width, height, System.currentTimeMillis());
        int pw = Math.min(540, width-36), ph = 304;
        int x = (width-pw)/2, y = (height-ph)/2;
        UiDraw.panel(g, x, y, pw, ph, 24, true);
        UiDraw.centered(g, font, "НОВЫЙ СЕРВЕР", width/2, y+24, 0xFFFFFFFF);
        UiDraw.text(g, font, "НАЗВАНИЕ СЕРВЕРА", x+32, y+54, 0xD2EAF7FF);
        UiDraw.panel(g, x+24, y+68, pw-48, 40, 10, false);
        UiDraw.text(g, font, "АДРЕС / IP", x+32, y+122, 0xD2EAF7FF);
        UiDraw.panel(g, x+24, y+136, pw-48, 40, 10, false);
        UiDraw.text(g, font, "Пример: play.example.net:25565", x+32, y+188, 0xA8FFFFFF);
        button(g, x+pw-176, y+ph-52, 144, 34, "СОХРАНИТЬ", mx, my, true);
        button(g, x+32, y+ph-52, 104, 34, "НАЗАД", mx, my, false);
        if (!error.isEmpty()) UiDraw.centered(g, font, error, width/2, y+ph-73, 0xFFFFC0A9);
        super.render(g, mx, my, partialTick);
    }

    private void button(GuiGraphics g,int x,int y,int w,int h,String label,int mx,int my,boolean primary) {
        boolean hover=mx>=x&&mx<=x+w&&my>=y&&my<=y+h;
        UiDraw.panel(g,x,y,w,h,11,hover||primary);
        UiDraw.centered(g,font,label,x+w/2,y+12,primary?0xFFFFFFFF:0xDFFFFFFF);
    }

    private void saveServer() {
        String serverName=name.getValue().trim(), host=address.getValue().trim();
        if(serverName.isBlank()) serverName=host;
        if(serverName.isBlank()||host.isBlank()){error="Заполните название и IP";return;}
        if(host.contains(" ")||host.length()>255){error="Проверьте адрес сервера";return;}
        ServerList servers=new ServerList(Minecraft.getInstance()); servers.load();
        for(int i=0;i<servers.size();i++) if(servers.get(i).ip.equalsIgnoreCase(host)){error="Этот адрес уже добавлен";return;}
        servers.add(new ServerData(serverName,host,ServerData.Type.OTHER),false);
        servers.save();
        minecraft.setScreen(new RealDlcHomeScreen(true));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick) {
        if(e.button()!=0) return super.mouseClicked(e,doubleClick);
        int pw=Math.min(540,width-36),ph=304,x=(width-pw)/2,y=(height-ph)/2;
        int by=y+ph-52;
        if(e.x()>=x+pw-176&&e.x()<=x+pw-32&&e.y()>=by&&e.y()<=by+34){saveServer();return true;}
        if(e.x()>=x+32&&e.x()<=x+136&&e.y()>=by&&e.y()<=by+34){minecraft.setScreen(new RealDlcHomeScreen(true));return true;}
        return super.mouseClicked(e,doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if(e.key()==GLFW.GLFW_KEY_ESCAPE){minecraft.setScreen(new RealDlcHomeScreen(true));return true;}
        if(e.key()==GLFW.GLFW_KEY_ENTER&&!name.isFocused()){saveServer();return true;}
        return super.keyPressed(e);
    }
    @Override public boolean shouldCloseOnEsc(){return false;}
    @Override public boolean isPauseScreen(){return true;}
}
