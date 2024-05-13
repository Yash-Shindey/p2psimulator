import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.*;

public class P2PNetworkSimulator extends JFrame implements ActionListener, MouseListener, ChangeListener {
    private static final long serialVersionUID = 1L;
    private BufferedImage display;
    private java.awt.image.BufferStrategy strategy;
    private static final int width = 1024, height = 768;
    private Network net;
    private int nodes = 0;
    private int maxnodes = 8;
    private double speedFactor = 1.0;
    private boolean pause = false;

    private JPanel controlPanel;
    private JButton addNodeButton;
    private JButton removeNodeButton;
    private JSlider speedSlider;
    private JButton changeColorButton;
    private JPanel canvasPanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            P2PNetworkSimulator window = new P2PNetworkSimulator();
            window.setupGUI();
        });
    }

    public void setupGUI() {
        setTitle("P2P Network Simulator");
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(5, 1, 10, 10));

        addNodeButton = new JButton("Add Node");
        addNodeButton.addActionListener(this);
        controlPanel.add(addNodeButton);

        removeNodeButton = new JButton("Subtract Node");
        removeNodeButton.addActionListener(this);
        controlPanel.add(removeNodeButton);

        speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 200, 100);
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setMinorTickSpacing(10);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.addChangeListener(this);
        controlPanel.add(speedSlider);

        changeColorButton = new JButton("Change Node Colour");
        changeColorButton.addActionListener(this);
        controlPanel.add(changeColorButton);

        add(controlPanel, BorderLayout.EAST);

        canvasPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintNetwork(g);
            }
        };
        canvasPanel.setBackground(Color.WHITE);
        add(canvasPanel, BorderLayout.CENTER);

        javax.swing.Timer clock = new javax.swing.Timer(10, this);
        clock.start();

        net = new Network(30, 0.01f, .1f, 0.2f, 100f, 12345);
        for (int k = 0; k < maxnodes; k++) {
            addNode();
        }

        setVisible(true);
        createBufferStrategy(2);
        strategy = getBufferStrategy();
    }

    public void addNode() {
        ArrayList<String> target = new ArrayList<>();
        if (nodes > 3) {
            for (int j = 0; j < 3; j++) {
                target.add(net.RandomNode());
            }
        }
        Node node = new TestNode("Node-" + nodes, target, 0.1 + Math.random(), 10);
        net.addNode(node, 300 + (float) Math.sin(nodes * 2 * Math.PI / maxnodes) * 200,
                300 + (float) Math.cos(nodes * 2 * Math.PI / maxnodes) * 200, 20);
        nodes++;
    }

    public void removeNode() {
        if (nodes > 0) {
            nodes--;
            net.stop("Node-" + nodes);
        }
    }

    public void adjustSpeed(double factor) {
        speedFactor *= factor;
        net.time_speed *= factor;
    }

    public void togglePause() {
        pause = !pause;
    }

    public void resetSimulation() {
        net.stop();
        nodes = 0;
        net = new Network(30, 0.01f, .1f, 0.2f, 100f, 12345);
        for (int k = 0; k < maxnodes; k++) {
            addNode();
        }
    }

    public void changeNodeColor() {
        Random rand = new Random();
        net.nodes.values().forEach(node -> {
            node.setColor(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
        });
    }

    public void paintNetwork(Graphics g) {
        net.draw(g, 2f);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addNodeButton) {
            addNode();
        } else if (e.getSource() == removeNodeButton) {
            removeNode();
        } else if (e.getSource() == changeColorButton) {
            changeNodeColor();
        }

        if (!pause) {
            canvasPanel.repaint();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocus();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        if (!source.getValueIsAdjusting()) {
            int speed = (int) source.getValue();
            net.time_speed = speed / 100.0;
        }
    }

    class Network implements Runnable {
        ConcurrentHashMap<String, Node> nodes;
        ConcurrentHashMap<String, Link> links;
        PriorityBlockingQueue<Transmission> transmit_queue;
        boolean stopped = false;
        Random rand;
        float default_link_rate;
        float latency_per_distance;
        float max_random_latency;
        float packet_drop_chance;
        float clock_desynchronization;
        double net_time = 0;
        long last_time = System.currentTimeMillis();
        public double time_speed = 1;

        public Network(float default_link_rate, float latency_per_distance, float max_random_latency,
                       float packet_drop_chance, float clock_desynchronization, int random_seed) {
            this.default_link_rate = default_link_rate;
            this.latency_per_distance = latency_per_distance;
            this.max_random_latency = max_random_latency;
            this.packet_drop_chance = packet_drop_chance;
            nodes = new ConcurrentHashMap<>();
            links = new ConcurrentHashMap<>();
            transmit_queue = new PriorityBlockingQueue<>();
            rand = new Random(random_seed);
            Thread t = new Thread(this);
            t.start();
        }

        public synchronized void addNode(Node n, float x, float y, float rate) {
            n.x = x;
            n.y = y;
            n.refill_rate = rate;
            n.maximum_flow = 10 * rate;
            n.flow = n.maximum_flow;
            n.last_time = getTime();
            n.clock_offset = (rand.nextFloat() * 2f - 1f) * clock_desynchronization;
            n.network = this;
            nodes.put(n.address, n);
            Thread t = new Thread(n);
            t.start();
        }

        public void setLink(String from, String to, float latency, float rate) {
            links.put(from + "-" + to, new Link(from, to, latency, rate, rate * 10, getTime()));
        }

        public double getTime() {
            long current_time = System.currentTimeMillis();
            net_time += (current_time - last_time) * time_speed / 1000.0;
            last_time = current_time;
            return net_time;
        }

        public void sendMessage(String from, String to, byte[] message) {
            Node f = nodes.get(from), t = nodes.get(to);
            if (f != null && !f.stopped && t != null && !t.stopped) {
                Link l = links.get(from + "-" + to);
                if (l == null) {
                    l = new Link(from, to,
                            latency_per_distance * distance(from, to) + rand.nextFloat() * max_random_latency,
                            default_link_rate, default_link_rate * 10, getTime());
                    links.put(from + "-" + to, l);
                }
                double sent = getTime();
                double arrival = Math.max(l.sendTime(message.length, sent), t.sendTime(message.length, sent));
                Transmission m = new Transmission(from, to, message, sent, arrival);
                transmit_queue.add(m);
            }
        }

        public void stop() {
            nodes.keySet().forEach(this::stop);
            stopped = true;
        }

        public void stop(String node) {
            nodes.get(node).stop();
        }

        public synchronized String RandomNode() {
            if (nodes.size() == 0) return "";
            int which = (int) (nodes.size() * rand.nextFloat());
            Iterator<String> i = nodes.keySet().iterator();
            int w = 0;
            while (w++ < which) i.next();
            return nodes.get(i.next()).address;
        }

        public void run() {
            while (!stopped) {
                double time = getTime();
                while (transmit_queue.size() > 0 && transmit_queue.peek().arrivaltime < time) {
                    Transmission m = transmit_queue.poll();
                    Node t = nodes.get(m.to);
                    if (t != null && !t.stopped && !m.dropped) {
                        t.receive(m.from, m.message);
                    }
                }
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                }
            }
        }

        public void draw(Graphics g, float inactive_time) {
            links.values().forEach(link -> link.draw(g, inactive_time));
            nodes.values().forEach(node -> node.draw(g));
            transmit_queue.forEach(m -> m.draw(g));
        }

        public float distance(String from, String to) {
            Node a = nodes.get(from);
            Node b = nodes.get(to);
            if (a == null || b == null) {
                return 99999999;
            } else {
                return (float) Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
            }
        }

        class Link {
            String from, to;
            public float latency;
            public float refill_rate;
            public float maximum_flow;
            public float flow;
            public double last_time;
            public double lastarrival;

            public Link(String from, String to, float latency, float refill, float max, double time) {
                this.from = from;
                this.to = to;
                this.latency = latency;
                this.refill_rate = refill;
                this.maximum_flow = max;
                this.last_time = time;
                this.flow = max;
            }

            public void updateFlow(double time) {
                flow += refill_rate * (time - last_time);
                if (flow > maximum_flow) {
                    flow = maximum_flow;
                }
                last_time = time;
            }

            public double sendTime(int size, double request_time) {
                updateFlow(request_time);
                double arrival = request_time + latency + Math.max((size - flow) / refill_rate, 0);
                flow -= size;
                lastarrival = arrival;
                return arrival;
            }

            public synchronized void draw(Graphics gr, float inactivitetime) {
                if (getTime() - lastarrival < inactivitetime) {
                    int r = 0, g = 0;
                    float mid = maximum_flow / 2;
                    if (flow < mid) {
                        r = (int) (255 * (mid - flow) / mid);
                    } else {
                        g = (int) (255 * (flow - mid) / mid);
                    }
                    gr.setColor(new Color(Math.min(r, 255), Math.min(g, 255), 0));
                    Node f = nodes.get(from), t = nodes.get(to);
                    if (f != null && t != null && !f.stopped && !t.stopped) {
                        int oy = 10;
                        if (f.address.compareTo(t.address) < 0) {
                            oy = -10;
                        }
                        gr.drawLine((int) f.x, (int) f.y + oy, (int) t.x, (int) t.y + oy);
                    }
                }
            }
        }

        class Transmission implements Comparable<Transmission> {
            double senttime;
            double arrivaltime;
            String from;
            String to;
            byte[] message;
            boolean dropped;

            public Transmission(String from, String to, byte[] message, double senttime, double arrivaltime) {
                this.from = from;
                this.to = to;
                this.message = message;
                this.senttime = senttime;
                this.arrivaltime = arrivaltime;
                dropped = rand.nextFloat() < packet_drop_chance;
            }

            public int compareTo(Transmission o) {
                return (int) (10000 * (arrivaltime - o.arrivaltime));
            }

            public synchronized void draw(Graphics g) {
                Node f = nodes.get(from), t = nodes.get(to);
                if (f != null && t != null && !f.stopped && !t.stopped) {
                    double s = (getTime() - senttime) / (float) (arrivaltime - senttime);
                    float x = (float) ((1 - s) * f.x + s * t.x), y = (float) ((1 - s) * f.y + s * t.y);
                    if (dropped) {
                        g.setColor(Color.red);
                    } else {
                        g.setColor(Color.black);
                    }
                    int oy = 10;
                    if (f.address.compareTo(t.address) < 0) {
                        oy = -10;
                    }
                    g.drawOval((int) x - 2, (int) (y - 2) + oy, 4, 4);
                }
            }
        }
    }

    abstract class Node implements Runnable {
        public String address;
        LinkedBlockingQueue<Message> message_queue;
        public boolean stopped = false;
        public Network network;
        public float x, y;
        public float refill_rate;
        public float maximum_flow;
        public float flow;
        public double last_time;
        public double clock_offset;
        private Color color;

        public Node(String address) {
            this.address = address;
            message_queue = new LinkedBlockingQueue<>();
            this.color = Color.GREEN; // Default color
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public void receive(String from, byte[] message) {
            message_queue.add(new Message(from, message));
        }

        public void send(String to, byte[] message) {
            if (network != null) {
                network.sendMessage(address, to, message);
            }
        }

        public double getTime() {
            return network.getTime() + clock_offset;
        }

        public void stop() {
            stopped = true;
        }

        public synchronized void draw(Graphics gr) {
            if (!stopped) {
                gr.setColor(this.color);
                gr.fillOval((int) x - 10, (int) y - 10, 20, 20);
                gr.setColor(Color.BLACK);
                gr.drawOval((int) x - 10, (int) y - 10, 20, 20);
            }
        }

        public double sendTime(int size, double requesttime) {
            updateFlow(requesttime);
            double arrival = requesttime + Math.max((size - flow) / refill_rate, 0);
            flow -= size;
            return arrival;
        }

        public void updateFlow(double time) {
            flow += refill_rate * (time - last_time);
            if (flow > maximum_flow) {
                flow = maximum_flow;
            }
            last_time = time;
        }
    }

    class TestNode extends Node {
        ArrayList<String> target;
        HashMap<String, Double> lastmessage;
        double wait, lasttime;
        int size;
        int which;

        public TestNode(String id, ArrayList<String> target, double wait, int size) {
            super(id);
            this.target = target;
            this.wait = wait;
            this.size = size;
            lastmessage = new HashMap<>();
        }

        public void run() {
            while (!stopped) {
                double time = getTime();
                if (time - lasttime > wait) {
                    if (which >= target.size()) {
                        which = 0;
                    } else {
                        if (!lastmessage.containsKey(target.get(which))) {
                            lastmessage.put(target.get(which), getTime());
                        }
                        double lasttime = lastmessage.get(target.get(which));
                        if (getTime() - lasttime > wait * 30) {
                            target.remove(which);
                        } else {
                            send(target.get(which), new byte[size]);
                            which++;
                        }
                    }
                    lasttime = time;
                }
                while (message_queue.peek() != null) {
                    Message m = message_queue.poll();
                    if (!target.contains(m.from)) {
                        target.add(m.from);
                    }
                    lastmessage.put(m.from, getTime());
                }
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    class Message {
        public String from;
        public byte[] message;

        public Message(String from, byte[] message) {
            this.from = from;
            this.message = message;
        }
    }
}
