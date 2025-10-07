package org.example;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class zadanie2 {
    int width = 800;
    int height = 600;

    long window;
    GLFWErrorCallback errorCallback;
    GLFWKeyCallback keyCallback;

    Random rand = new Random();

    // Triangle vertices
    Point[] triangle = new Point[3];
    Color[] colors = new Color[3];

    class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    class Color {
        float r, g, b;
        Color(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    void spusti() {
        try {
            init();
            GL.createCapabilities();
            generateRandomTriangle();
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

        window = glfwCreateWindow(width, height, "UGR03_2 - Triangle Fill", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Chyba pri vytvoreni GLFW okna!!!");

        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);
                else if (key == GLFW_KEY_SPACE && action == GLFW_RELEASE) {
                    generateRandomTriangle();
                }
            }
        });

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);
    }

    void generateRandomTriangle() {
        int padding = 100;

        // генеруємо точки всередині екрана
        for (int i = 0; i < 3; i++) {
            triangle[i] = new Point(
                    rand.nextInt(width - 2 * padding) + padding,
                    rand.nextInt(height - 2 * padding) + padding
            );
            colors[i] = new Color(
                    rand.nextFloat(),
                    rand.nextFloat(),
                    rand.nextFloat()
            );
        }

        // перевірка на "площинність" (усі три точки на одній лінії)
        int area2 = triangle[0].x * (triangle[1].y - triangle[2].y)
                + triangle[1].x * (triangle[2].y - triangle[0].y)
                + triangle[2].x * (triangle[0].y - triangle[1].y);

        if (Math.abs(area2) < 2000) {
            // занадто плаский — згенеруємо знову
            generateRandomTriangle();
        }
    }

    // Bresenham line algorithm - returns list of points
    List<Point> bresenhamLine(int x0, int y0, int x1, int y1) {
        List<Point> points = new ArrayList<>();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (true) {
            points.add(new Point(x, y));

            if (x == x1 && y == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }

        return points;
    }

    // Interpolate color between two colors based on parameter t (0 to 1)
    Color interpolateColor(Color c1, Color c2, float t) {
        return new Color(
                c1.r + (c2.r - c1.r) * t,
                c1.g + (c2.g - c1.g) * t,
                c1.b + (c2.b - c1.b) * t
        );
    }

    void fillTriangle() {
        // Сортуємо вершини за координатою Y
        Point[] p = triangle.clone();
        Color[] c = colors.clone();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2 - i; j++) {
                if (p[j].y > p[j + 1].y) {
                    Point tmpP = p[j];
                    p[j] = p[j + 1];
                    p[j + 1] = tmpP;

                    Color tmpC = c[j];
                    c[j] = c[j + 1];
                    c[j + 1] = tmpC;
                }
            }
        }

        Point p0 = p[0], p1 = p[1], p2 = p[2];
        Color c0 = c[0], c1 = c[1], c2 = c[2];

        // Функція інтерполяції x для даного y
        java.util.function.BiFunction<Point, Point, Float> slope = (a, b) ->
                (b.y != a.y) ? (float)(b.x - a.x) / (b.y - a.y) : 0f;

        float dx01 = slope.apply(p0, p1);
        float dx02 = slope.apply(p0, p2);
        float dx12 = slope.apply(p1, p2);

        float sx1 = p0.x;
        float sx2 = p0.x;

        glBegin(GL_POINTS);

        // Верхня частина (від p0 до p1)
        for (int y = p0.y; y <= p1.y; y++) {
            int xStart = (int) Math.min(sx1, sx2);
            int xEnd = (int) Math.max(sx1, sx2);

            float tTop = (p1.y != p0.y) ? (float)(y - p0.y) / (p1.y - p0.y) : 0;
            float tBottom = (p2.y != p0.y) ? (float)(y - p0.y) / (p2.y - p0.y) : 0;

            Color leftColor = interpolateColor(c0, c1, tTop);
            Color rightColor = interpolateColor(c0, c2, tBottom);

            for (int x = xStart; x <= xEnd; x++) {
                float tH = (xEnd != xStart) ? (float)(x - xStart) / (xEnd - xStart) : 0;
                Color fillColor = interpolateColor(leftColor, rightColor, tH);
                glColor3f(fillColor.r, fillColor.g, fillColor.b);
                glVertex2i(x, y);
            }

            sx1 += dx01;
            sx2 += dx02;
        }

        // Нижня частина (від p1 до p2)
        sx1 = p1.x;
        sx2 = p0.x + dx02 * (p1.y - p0.y);

        for (int y = p1.y; y <= p2.y; y++) {
            int xStart = (int) Math.min(sx1, sx2);
            int xEnd = (int) Math.max(sx1, sx2);

            float tTop = (p2.y != p1.y) ? (float)(y - p1.y) / (p2.y - p1.y) : 0;
            float tBottom = (p2.y != p0.y) ? (float)(y - p0.y) / (p2.y - p0.y) : 0;

            Color leftColor = interpolateColor(c1, c2, tTop);
            Color rightColor = interpolateColor(c0, c2, tBottom);

            for (int x = xStart; x <= xEnd; x++) {
                float tH = (xEnd != xStart) ? (float)(x - xStart) / (xEnd - xStart) : 0;
                Color fillColor = interpolateColor(leftColor, rightColor, tH);
                glColor3f(fillColor.r, fillColor.g, fillColor.b);
                glVertex2i(x, y);
            }

            sx1 += dx12;
            sx2 += dx02;
        }

        glEnd();

        // Контур трикутника
        glColor3f(1, 1, 1);
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < 3; i++) {
            glVertex2i(triangle[i].x, triangle[i].y);
        }
        glEnd();
    }


    void vykresliGL() {
        fillTriangle();
    }

    void loop() {
        glViewport(0, 0, width, height);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(-0.5, width - 0.5, height - 0.5, -0.5, 0, 1);

        glShadeModel(GL_FLAT);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glClearColor(0.f, 0.f, 0.f, 1.f);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            vykresliGL();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new zadanie2().spusti();
    }
}