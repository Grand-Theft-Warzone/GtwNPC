package fr.aym.gtwnpc.utils;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import org.joml.Intersectionf;
import org.joml.Matrix3f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class OOBB {
    private final Vector3f center; // Centre de la boîte
    private final Vector3f halfExtents; // Demi-longueurs des arêtes de la boîte
    private final Matrix3f orientation; // Orientation de la boîte représentée par une matrice de rotation

    // Constructeur
    public OOBB(Vector3f center, Vector3f halfExtents, Matrix3f orientation) {
        this.center = new Vector3f(center);
        this.halfExtents = new Vector3f(halfExtents);
        this.orientation = new Matrix3f(orientation);
    }

    public boolean collidesWithOOBB(OOBB other) {
        // Récupération de l'orientation des boîtes sous forme de vecteurs d'axes
        Vector3f[] axesThis = getAxes(this.orientation);
        Vector3f[] axesOther = getAxes(other.orientation);

        // Test des axes de "this"
        for (Vector3f axis : axesThis) {
            if (!overlapOnAxis(this, other, axis)) {
                return false; // Pas de collision si pas de chevauchement sur un des axes de "this"
            }
        }

        // Test des axes de "other"
        for (Vector3f axis : axesOther) {
            if (!overlapOnAxis(this, other, axis)) {
                return false; // Pas de collision si pas de chevauchement sur un des axes de "other"
            }
        }

        // Test des axes croisés
        for (Vector3f axisThis : axesThis) {
            for (Vector3f axisOther : axesOther) {
                Vector3f crossAxis = axisThis.cross(axisOther, new Vector3f());
                if (crossAxis.lengthSquared() > 1E-8f) { // Éviter les axes nuls (parallèles)
                    if (!overlapOnAxis(this, other, crossAxis.normalize())) {
                        return false; // Pas de collision si pas de chevauchement sur un axe croisé
                    }
                }
            }
        }

        return true; // Collision si tous les tests de chevauchement sont passés
    }

    private boolean overlapOnAxis(OOBB one, OOBB two, Vector3f axis) {
        // Projections des demi-étendues de "one" et "two" sur l'axe
        float oneProject = project(one, axis);
        float twoProject = project(two, axis);
        // Projections des centres sur l'axe
        float distance = Math.abs(one.center.dot(axis) - two.center.dot(axis));
        // Il y a chevauchement si la somme des projections des demi-étendues est supérieure à la distance entre les centres
        return distance <= (oneProject + twoProject);
    }

    private float project(OOBB box, Vector3f axis) {
        // Projections des demi-étendues de la boîte sur l'axe
        return box.halfExtents.x * Math.abs(axis.dot(getAxes(box.orientation)[0])) +
                box.halfExtents.y * Math.abs(axis.dot(getAxes(box.orientation)[1])) +
                box.halfExtents.z * Math.abs(axis.dot(getAxes(box.orientation)[2]));
    }

    private Vector3f[] getAxes(Matrix3f orientation) {
        // Extraction des axes locaux de la boîte à partir de l'orientation
        Vector3f xAxis = new Vector3f(orientation.m00(), orientation.m10(), orientation.m20());
        Vector3f yAxis = new Vector3f(orientation.m01(), orientation.m11(), orientation.m21());
        Vector3f zAxis = new Vector3f(orientation.m02(), orientation.m12(), orientation.m22());
        return new Vector3f[]{xAxis, yAxis, zAxis};
    }


    // Détection de collision avec une AABB
    public boolean collidesWithAABB(AxisAlignedBB other) {
        // Convertir l'AABB en OOBB avec une orientation identité pour réutiliser collidesWithOOBB
        Matrix3f identity = new Matrix3f(); // Matrice d'identité pour l'orientation
        Vector3f otherHalf = new Vector3f((float) (other.maxX - other.minX) / 2, (float) (other.maxY - other.minY) / 2, (float) (other.maxZ - other.minZ) / 2);
        Vector3f otherCenter = new Vector3f((float) (other.maxX + other.minX) / 2, (float) (other.maxY + other.minY) / 2, (float) (other.maxZ + other.minZ) / 2);
        OOBB convertedAABB = new OOBB(otherCenter, otherHalf, identity);
        return this.collidesWithOOBB(convertedAABB);
    }

    // Test si un point est contenu dans l'OOBB
    public boolean containsPoint(Vector3f point) {
        Vector3f localPoint = point.sub(center, new Vector3f()).mul(orientation.transpose(new Matrix3f()));
        return Math.abs(localPoint.x) <= halfExtents.x &&
                Math.abs(localPoint.y) <= halfExtents.y &&
                Math.abs(localPoint.z) <= halfExtents.z;
    }

    // Getters et Setters si nécessaire...

    public Vector3f getCenter() {
        return center;
    }

    public Vector3f getHalfExtents() {
        return halfExtents;
    }

    public Matrix3f getOrientation() {
        return orientation;
    }

    public AxisAlignedBB toAABB() {
        //AxisAlignedBB bb = new AxisAlignedBB(center.x - halfExtents.x, center.y - halfExtents.y, center.z - halfExtents.z, center.x + halfExtents.x, center.y + halfExtents.y, center.z + halfExtents.z);
        com.jme3.math.Matrix3f jm3Rot = new com.jme3.math.Matrix3f(orientation.m00(), orientation.m01(), orientation.m02(), orientation.m10(), orientation.m11(), orientation.m12(), orientation.m20(), orientation.m21(), orientation.m22());
        com.jme3.math.Quaternion quatRot = new com.jme3.math.Quaternion();
        quatRot.fromRotationMatrix(jm3Rot);
        com.jme3.math.Vector3f v1 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(-halfExtents.x, 0, 0), quatRot);
        com.jme3.math.Vector3f v2 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(0, -halfExtents.y, 0), quatRot);
        com.jme3.math.Vector3f v3 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(0, 0, -halfExtents.z), quatRot);
        com.jme3.math.Vector3f v4 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(halfExtents.x, 0, 0), quatRot);
        com.jme3.math.Vector3f v5 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(0, halfExtents.y, 0), quatRot);
        com.jme3.math.Vector3f v6 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(0, 0, halfExtents.z), quatRot);
        MutableBoundingBox n = new MutableBoundingBox(DynamXMath.getMin(v1.x, v2.x, v3.x, v4.x, v5.x, v6.x), DynamXMath.getMin(v1.y, v2.y, v3.y, v4.y, v5.y, v6.y), DynamXMath.getMin(v1.z, v2.z, v3.z, v4.z, v5.z, v6.z),
                DynamXMath.getMax(v1.x, v2.x, v3.x, v4.x, v5.x, v6.x), DynamXMath.getMax(v1.y, v2.y, v3.y, v4.y, v5.y, v6.y), DynamXMath.getMax(v1.z, v2.z, v3.z, v4.z, v5.z, v6.z));
        return n.offset(center.x, center.y, center.z).toBB();
    }

    public void drawOOBB(float red, float green, float blue, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(center.x, center.y, center.z);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
        com.jme3.math.Matrix3f jm3Rot = new com.jme3.math.Matrix3f(orientation.m00(), orientation.m01(), orientation.m02(), orientation.m10(), orientation.m11(), orientation.m12(), orientation.m20(), orientation.m21(), orientation.m22());
        com.jme3.math.Quaternion quatRot = new com.jme3.math.Quaternion();
        quatRot.fromRotationMatrix(jm3Rot);
        com.jme3.math.Vector3f v1 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(-halfExtents.x, -halfExtents.y, -halfExtents.z), quatRot);
        com.jme3.math.Vector3f v2 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(-halfExtents.x, -halfExtents.y, halfExtents.z), quatRot);
        com.jme3.math.Vector3f v3 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(-halfExtents.x, halfExtents.y, -halfExtents.z), quatRot);
        com.jme3.math.Vector3f v4 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(-halfExtents.x, halfExtents.y, halfExtents.z), quatRot);
        com.jme3.math.Vector3f v5 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(halfExtents.x, -halfExtents.y, -halfExtents.z), quatRot);
        com.jme3.math.Vector3f v6 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(halfExtents.x, -halfExtents.y, halfExtents.z), quatRot);
        com.jme3.math.Vector3f v7 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(halfExtents.x, halfExtents.y, -halfExtents.z), quatRot);
        com.jme3.math.Vector3f v8 = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get(halfExtents.x, halfExtents.y, halfExtents.z), quatRot);
        buffer.pos(v1.x, v1.y, v1.z).color(red, green, blue, 0.0F).endVertex();
        buffer.pos(v1.x, v1.y, v1.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v5.x, v5.y, v5.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v6.x, v6.y, v6.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v2.x, v2.y, v2.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v1.x, v1.y, v1.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v3.x, v3.y, v3.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v7.x, v7.y, v7.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v8.x, v8.y, v8.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v4.x, v4.y, v4.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v3.x, v3.y, v3.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v4.x, v4.y, v4.z).color(red, green, blue, 0.0F).endVertex();
        buffer.pos(v2.x, v2.y, v2.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v8.x, v8.y, v8.z).color(red, green, blue, 0.0F).endVertex();
        buffer.pos(v6.x, v6.y, v6.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v7.x, v7.y, v7.z).color(red, green, blue, 0.0F).endVertex();
        buffer.pos(v5.x, v5.y, v5.z).color(red, green, blue, alpha).endVertex();
        buffer.pos(v5.x, v5.y, v5.z).color(red, green, blue, 0.0F).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    public Vector3f calculateIntercept(Vector3f start, Vector3f end) {
        // Inverser l'orientation par transposition (pour une matrice de rotation pure)
        Matrix3f inverseOrientation = new Matrix3f(orientation).transpose();
        // Appliquer l'inverse de la transformation pour ramener le rayon dans l'espace local de l'OOBB
        Vector3f localStart = inverseOrientation.transform(new Vector3f(start).sub(center));
        Vector3f localEnd = inverseOrientation.transform(new Vector3f(end).sub(center));

        // Direction du rayon dans l'espace local
        Vector3f dir = new Vector3f(localEnd).sub(localStart).normalize();

        // Calcul de l'intersection avec l'AABB dans l'espace local
        Vector2f t = new Vector2f();
        boolean intersects = Intersectionf.intersectRayAab(localStart, dir, new Vector3f(-halfExtents.x, -halfExtents.y, -halfExtents.z), new Vector3f(halfExtents.x, halfExtents.y, halfExtents.z), t);

        if (intersects && t.x < t.y) {
            // Si intersection, calculer le point d'intersection exact dans l'espace local
            Vector3f localIntersection = new Vector3f(dir).mul(t.x).add(localStart);

            // Appliquer la transformation pour ramener le point d'intersection dans l'espace global
            Vector3f globalIntersection = orientation.transform(localIntersection).add(center);
            return globalIntersection;
        }

        // Si pas d'intersection, retourner null
        return null;
    }
}
