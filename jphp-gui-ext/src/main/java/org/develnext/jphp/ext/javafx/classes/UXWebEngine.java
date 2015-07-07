package org.develnext.jphp.ext.javafx.classes;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import org.develnext.jphp.ext.javafx.JavaFXExtension;
import org.develnext.jphp.ext.javafx.support.EventProvider;
import org.w3c.dom.Document;
import php.runtime.Memory;
import php.runtime.annotation.Reflection;
import php.runtime.annotation.Reflection.*;
import php.runtime.env.Environment;
import php.runtime.exceptions.CriticalException;
import php.runtime.invoke.Invoker;
import php.runtime.lang.BaseWrapper;
import php.runtime.memory.ArrayMemory;
import php.runtime.reflection.ClassEntity;

@Abstract
@Name(JavaFXExtension.NS + "UXWebEngine")
public class UXWebEngine extends BaseWrapper<WebEngine> {
    interface WrappedInterface {
        @Property Document document();
        @Property boolean javaScriptEnabled();
        @Property String location();
        @Property String title();

        @Property String userStyleSheetLocation();

        void load(String url);
        void loadContent(String content);
        void loadContent(String content, String contentType);

        void reload();
    }

    public UXWebEngine(Environment env, WebEngine wrappedObject) {
        super(env, wrappedObject);
    }

    public UXWebEngine(Environment env, ClassEntity clazz) {
        super(env, clazz);
    }

    @Signature
    public Memory executeScript(Environment env, String script) {
        return Memory.wrap(env, getWrappedObject().executeScript(script));
    }

    @Signature
    public Object callFunction(Environment env, String name, ArrayMemory args) {
        JSObject window = (JSObject) getWrappedObject().executeScript("window");

        if (window == null) {
            throw new IllegalStateException("Unable to find window object");
        }

        return window.call(name, args.toStringArray());
    }

    private static class Bridge {
        protected final Invoker handler;

        public Bridge(Invoker handler) {
            this.handler = handler;
        }

        public void run() {
            handler.callAny();
        }
    }

    @Signature
    public void addBridge(Environment env, String name, final Invoker handler) {
        JSObject window = (JSObject) getWrappedObject().executeScript("window");

        window.setMember(name, new Bridge(handler));
    }

    @Getter
    public Worker.State getState() {
        return getWrappedObject().getLoadWorker().getState();
    }

    @Signature
    public void waitState(final Worker.State state, final Invoker invoker) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final boolean[] done = {false};

                    while (true) {

                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                done[0] = getWrappedObject().getLoadWorker().getState() == state;
                            }
                        });

                        if (!done[0]) {
                            Thread.sleep(50);
                            continue;
                        }

                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                invoker.callAny(UXWebEngine.this);
                            }
                        });

                        break;
                    }
                } catch (InterruptedException e) {
                    throw new CriticalException(e);
                }
            }
        });

        thread.start();
    }

    @Signature
    @SuppressWarnings("unchecked")
    public void on(String event, Invoker invoker, String group) {
        Object target = getWrappedObject();
        EventProvider eventProvider = EventProvider.get(target, event);

        if (eventProvider != null) {
            eventProvider.on(target, event, group, invoker);
        } else {
            throw new IllegalArgumentException("Unable to find the '"+event+"' event type");
        }
    }

    @Signature
    public void on(String event, Invoker invoker) {
        on(event, invoker, "general");
    }

    @Signature
    @SuppressWarnings("unchecked")
    public void off(String event, @Reflection.Nullable String group) {
        Object target = getWrappedObject();
        EventProvider eventProvider = EventProvider.get(target, event);

        if (eventProvider != null) {
            eventProvider.off(target, event, group);
        } else {
            throw new IllegalArgumentException("Unable to find the '"+event+"' event type");
        }
    }

    @Signature
    public void off(String event) {
        off(event, null);
    }

    @Signature
    public void trigger(String event, @Reflection.Nullable Event e) {
        Object target = getWrappedObject();
        EventProvider eventProvider = EventProvider.get(target, event);

        if (eventProvider != null) {
            eventProvider.trigger(target, event, e);
        } else {
            throw new IllegalArgumentException("Unable to find the '"+event+"' event type");
        }
    }
}
