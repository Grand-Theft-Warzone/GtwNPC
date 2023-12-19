package fr.aym.gtwnpc.network;

import fr.aym.acslib.utils.packetserializer.ISerializablePacket;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.path.NodeType;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.path.PathNodesManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BBMessagePathNodes implements ISerializablePacket {
    private Action action;
    private NodeType nodeType;
    private List<UUID> ids;
    private Collection<PathNode> nodes;

    public BBMessagePathNodes() {
    }

    public BBMessagePathNodes(NodeType nodeType, PathNode node) {
        this.action = Action.ADD;
        this.nodeType = nodeType;
        this.nodes = Collections.singletonList(node);
    }

    public BBMessagePathNodes(NodeType nodeType, Collection<PathNode> nodes) {
        this.action = Action.ADD_ALL;
        this.nodeType = nodeType;
        this.nodes = nodes;
    }

    public BBMessagePathNodes(Action action, NodeType nodeType, List<UUID> ids) {
        this.action = action;
        this.nodeType = nodeType;
        this.ids = ids;
    }

    @Override
    public Object[] getObjectsToSave() {
        return action == Action.ADD || action == Action.ADD_ALL ? new Object[]{action, nodeType, nodes} : new Object[]{action, nodeType, ids};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        action = (Action) objects[0];
        nodeType = (NodeType) objects[1];
        if (action != Action.ADD_ALL && action != Action.ADD)
            ids = (List<UUID>) objects[2];
        else
            nodes = (List<PathNode>) objects[2];
    }

    public static class HandlerClient implements IMessageHandler<BBMessagePathNodes, IMessage> {
        @Override
        public IMessage onMessage(BBMessagePathNodes message, MessageContext ctx) {
            System.out.println("Received nodes from server : " + message.action + " " + message.nodeType + " " + message.ids + " " + message.nodes);
            PathNodesManager manager = message.nodeType.getManager();
            Minecraft.getMinecraft().addScheduledTask(() -> {
                switch (message.action) {
                    case ADD:
                    case ADD_ALL:
                        message.nodes.forEach(node -> node.create(manager, false));
                        break;
                    case REMOVE:
                        message.ids.forEach(id -> {
                            if (manager.hasNode(id))
                                manager.getNode(id).delete(manager, false);
                            else
                                GtwNpcMod.log.warn("Cannot remove node: node does not exist: " + id);
                        });
                        break;
                    case LINK_NODES:
                        if (manager.hasNode(message.ids.get(0)) && manager.hasNode(message.ids.get(1)))
                            manager.getNode(message.ids.get(0)).addNeighbor(manager, manager.getNode(message.ids.get(1)), false);
                        else
                            GtwNpcMod.log.warn("Cannot link nodes: one of the nodes does not exist: " + message.ids.get(0) + " or " + message.ids.get(1) + " : " + manager.getNode(message.ids.get(0)) + " " + manager.getNode(message.ids.get(1)));
                        break;
                    case UNLINK_NODES:
                        if (manager.hasNode(message.ids.get(0)) && manager.hasNode(message.ids.get(1)))
                            manager.getNode(message.ids.get(0)).removeNeighbor(manager, manager.getNode(message.ids.get(1)), false);
                        else
                            GtwNpcMod.log.warn("Cannot unlink nodes: one of the nodes does not exist: " + message.ids.get(0) + " or " + message.ids.get(1) + " : " + manager.getNode(message.ids.get(0)) + " " + manager.getNode(message.ids.get(1)));
                        break;
                }
            });
            return null;
        }
    }

    public static class HandlerServer implements IMessageHandler<BBMessagePathNodes, IMessage> {
        @Override
        public IMessage onMessage(BBMessagePathNodes message, MessageContext ctx) {
            if (!ctx.getServerHandler().player.canUseCommand(4, "gtwnpc.pathnodes")) {
                ctx.getServerHandler().player.sendMessage(new TextComponentString("You don't have the permission to modify npc nodes !"));
                GtwNpcMod.log.warn(ctx.getServerHandler().player.getName() + " tried to modify npc nodes without permission !");
                return null;
            }
            PathNodesManager manager = message.nodeType.getManager();
            switch (message.action) {
                case ADD:
                    message.nodes.forEach(node -> node.create(manager, false));
                    GtwNpcMod.network.sendToAll(new BBMessagePathNodes(message.nodeType, message.nodes));
                    break;
                case REMOVE:
                    message.ids.forEach(id -> {
                        if (manager.hasNode(id))
                            manager.getNode(id).delete(manager, false);
                        else
                            GtwNpcMod.log.warn("Cannot remove node: node does not exist: " + id);
                    });
                    GtwNpcMod.network.sendToAll(new BBMessagePathNodes(message.action, message.nodeType, message.ids));
                    break;
                case ADD_ALL:
                    throw new IllegalArgumentException("Cannot add all nodes from client");
                case LINK_NODES:
                    if (manager.hasNode(message.ids.get(0)) && manager.hasNode(message.ids.get(1)))
                        manager.getNode(message.ids.get(0)).addNeighbor(manager, manager.getNode(message.ids.get(1)), false);
                    else
                        GtwNpcMod.log.warn("Cannot link nodes: one of the nodes does not exist: " + message.ids.get(0) + " or " + message.ids.get(1) + " : " + manager.getNode(message.ids.get(0)) + " " + manager.getNode(message.ids.get(1)));
                    GtwNpcMod.network.sendToAll(new BBMessagePathNodes(message.action, message.nodeType, message.ids));
                    break;
                case UNLINK_NODES:
                    if (manager.hasNode(message.ids.get(0)) && manager.hasNode(message.ids.get(1)))
                        manager.getNode(message.ids.get(0)).removeNeighbor(manager, manager.getNode(message.ids.get(1)), false);
                    else
                        GtwNpcMod.log.warn("Cannot unlink nodes: one of the nodes does not exist: " + message.ids.get(0) + " or " + message.ids.get(1) + " : " + manager.getNode(message.ids.get(0)) + " " + manager.getNode(message.ids.get(1)));
                    GtwNpcMod.network.sendToAll(new BBMessagePathNodes(message.action, message.nodeType, message.ids));
                    break;
            }
            return null;
        }
    }

    public enum Action {
        ADD,
        REMOVE,
        ADD_ALL,
        LINK_NODES,
        UNLINK_NODES
    }
}
