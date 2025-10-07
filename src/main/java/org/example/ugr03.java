package org.example;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public class ugr03 {
    private int sirka = 800;
    private int vyska = 600;
    private int rozmer = Math.min(sirka, vyska)/2; //max. polomer kruhu
    private int sx = sirka/2, sy = vyska/2; //stred obrazovky
    private ByteBuffer buffer = BufferUtils.createByteBuffer(sirka*vyska*4); //4 bpp (bytes per pixel), RGBA
    private int rgba_WHITE = 0xFFFFFFFF; //-1
    private int rgba_BLACK = 0x00000000;

    private void vypln(int x, int y) {
        bod(x,y);
    }

    void vykresliGL() {
        // Clear the screen and depth buffer
        glLoadIdentity();

        //vonkajsia lomena ciara
        glBegin(GL_LINE_LOOP);
        glColor3f(1, 1, 1);
        lomenaCiaraKruh(0.75, 0.25);
        glEnd();

        //vnutorna lomena ciara
        glBegin(GL_LINE_LOOP);
        glColor3f(0, 0, 1);
        lomenaCiaraKruh(0.2, 0.18);
        glEnd();

        takeScreenShot();

        glBegin(GL_POINTS);
        glColor3f(1, 0, 0);
        vypln(sx, sy);
        glEnd();
    }

    long window;
    GLFWErrorCallback errorCallback;
    GLFWKeyCallback   keyCallback;

    void spusti() {
        try {
            init();
            loop();

            glfwDestroyWindow(window);
            keyCallback.free();
        } finally {
            glfwTerminate();
            errorCallback.free();
        }
    }

    void init() {
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        if (!glfwInit())
            throw new IllegalStateException("Chyba pri inicializacii GLFW!!!");

        window = glfwCreateWindow(sirka, vyska, "UGR1", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Chyba pri vytvoreni GLFW okna!!!");

        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key,
                               int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);
            }
        });

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - sirka) / 2, (vidmode.height() - vyska) / 2);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);

        GL.createCapabilities();
        glReadBuffer(GL_BACK); //select a color buffer source for pixels
    }

    private void takeScreenShot() {
        glReadPixels(0, 0, sirka, vyska, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
    }

    private void riadok(int y, int x1, int x2) {
        if (x1 > x2) {
            x1=x1^x2; x2=x1^x2; x1=x1^x2;
        }
        for (int x=x1; x <=x2; x++) {
            bod(x, y);
        }
    }

    private void bod(int x, int y) {
        glVertex2i(x, y);
        y = vyska-1-y;
        buffer.asIntBuffer().put(y*sirka+x, rgba_WHITE);
    }

    private int zistiFarbu(int x, int y) {
        y = vyska-1-y;
        return buffer.asIntBuffer().get(sirka*y+x);
    }

    private void lomenaCiaraKruh(double stred, double odchylka) {
        double r, znam = -1;
        for (double uhol=0; uhol < 1.999*Math.PI; uhol += 20*Math.PI/180) {
            r = stred*rozmer+znam*odchylka*Math.random()*rozmer;
            glVertex2d(sx+r*Math.cos(uhol), sy+r*Math.sin(uhol));
            znam = -znam;
        }
    }

    void loop() {
        glViewport(0,0,sirka,vyska);

        glMatrixMode( GL_PROJECTION );
        glLoadIdentity();
        glOrtho( -0.5, sirka-0.5, vyska-0.5, -0.5, 0, 1);

        glMatrixMode( GL_MODELVIEW );
        glLoadIdentity();

        glClearColor( 0.f, 0.f, 0.f, 1.f ); //Initialize clear color

        vykresliGL();

        glfwSwapBuffers(window);

        while ( !glfwWindowShouldClose(window) ) {
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new ugr03().spusti();
    }
}
