package scr;

import java.util.*;

/**
 * @brief Rappresenta un albero KD per la ricerca dei vicini più prossimi.
 * 
 * La classe costruisce un KD-Tree a partire da un insieme di punti (Sample)
 * e consente di effettuare ricerche dei k-nearest neighbors in modo efficiente.
 */
public class KDTree {

    private KDNode root;
    private int dimensions;

    /**
     * @brief Costruttore del KDTree.
     * @param points Lista di Sample da cui costruire l'albero KD.
     * @throws IllegalArgumentException se la lista di punti è vuota.
     */
    public KDTree(List<Sample> points) {
        if (points.isEmpty()) {
            throw new IllegalArgumentException("Points list cannot be empty");
        }
        this.dimensions = points.get(0).features.length;
        root = buildTree(points, 0);
    }

    /**
     * @brief Nodo interno dell'albero KD.
     */
    private static class KDNode {
        Sample point;
        KDNode left, right;

        /**
        * @brief Costruttore di un nodo KD.
        * @param point Punto (Sample) associato al nodo.
        */
        KDNode(Sample point) {
            this.point = point;
        }
    }


    /**
     * @brief Costruisce ricorsivamente il KD-Tree.
     * @param points Lista di Sample da usare per costruire l'albero.
     * @param depth Profondità corrente dell'albero.
     * @return Nodo radice del sottoalbero costruito.
     */
    private KDNode buildTree(List<Sample> points, int depth) {
        if (points.isEmpty()) return null;

        int axis = depth % dimensions;
        points.sort(Comparator.comparingDouble(p -> p.features[axis]));
        int medianIndex = points.size() / 2;

        KDNode node = new KDNode(points.get(medianIndex));
        node.left = buildTree(points.subList(0, medianIndex), depth + 1);
        node.right = buildTree(points.subList(medianIndex + 1, points.size()), depth + 1);
        return node;
    }

    /**
     * @brief Restituisce i k vicini più prossimi a un dato Sample.
     * @param target Il punto di riferimento per la ricerca.
     * @param k Numero di vicini da trovare.
     * @return Lista dei k vicini più prossimi.
     */
    public List<Sample> kNearestNeighbors(Sample target, int k) {
        PriorityQueue<Sample> pq = new PriorityQueue<>(k, Comparator.comparingDouble(target::distance).reversed());
        kNearestNeighbors(root, target, k, 0, pq);
        return new ArrayList<>(pq);
    }

     /**
     * @brief Funzione ricorsiva per cercare i k vicini più prossimi.
     * @param node Nodo corrente dell'albero.
     * @param target Punto di riferimento.
     * @param k Numero di vicini da trovare.
     * @param depth Profondità corrente dell'albero.
     * @param pq Coda di priorità che mantiene i k migliori vicini trovati.
     */
    private void kNearestNeighbors(KDNode node, Sample target, int k, int depth, PriorityQueue<Sample> pq) {
        if (node == null) return;

        double distance = target.distance(node.point);
        if (pq.size() < k) {
            pq.offer(node.point);
        } else if (distance < target.distance(pq.peek())) {
            pq.poll();
            pq.offer(node.point);
        }

        int axis = depth % dimensions;
        KDNode near = target.features[axis] < node.point.features[axis] ? node.left : node.right;
        KDNode far = (near == node.left) ? node.right : node.left;

        kNearestNeighbors(near, target, k, depth + 1, pq);

        if (pq.size() < k || Math.abs(target.features[axis] - node.point.features[axis]) < target.distance(pq.peek())) {
            kNearestNeighbors(far, target, k, depth + 1, pq);
        }
    }
}