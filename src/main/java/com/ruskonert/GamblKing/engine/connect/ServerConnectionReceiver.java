package com.ruskonert.GamblKing.engine.connect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.ruskonert.GamblKing.engine.GameServer;
import com.ruskonert.GamblKing.engine.framework.entity.PlayerFramework;
import com.ruskonert.GamblKing.MessageType;
import com.ruskonert.GamblKing.adapter.PlayerAdapter;
import com.ruskonert.GamblKing.connect.packet.LoginConnectionPacket;
import com.ruskonert.GamblKing.connect.packet.RegisterConnectionPacket;
import com.ruskonert.GamblKing.entity.Player;
import com.ruskonert.GamblKing.framework.RoomFramework;
import com.ruskonert.GamblKing.property.ServerProperty;
import com.ruskonert.GamblKing.util.SecurityUtil;
import com.ruskonert.GamblKing.util.SystemUtil;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

public class ServerConnectionReceiver
{
    private Socket socket;
    public Socket getSocket() { return this.socket; }

    private DataInputStream in;
    public DataInputStream getInputStream() { return in; }

    private DataOutputStream out;
    public DataOutputStream getOutputStream() { return out; }

    private InetAddress address;

    ServerConnectionReceiver(Socket socket) throws IOException
    {
        this.socket = socket;
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        this.address = socket.getInetAddress();
        ServerConnectionReceiver.join(this.address, out);
        GameServer.getServer().getConsoleSender().log("The class ServerConnectionReceiver() was initialized from " + socket.getInetAddress().getHostAddress());
    }

    public static void join(InetAddress address, DataOutputStream out)
    {
        GameServer.getServer().getConsoleSender().log(address.getHostAddress() + " connected the login server");
        ConnectionBackground.getClientMap().put(address.getHostAddress(), out);
    }

    public static void leave(InetAddress address)
    {
        leave(address.getHostAddress());
    }

    public static void leave(String address)
    {
        ConnectionBackground.getClientMap().remove(address);
        GameServer.getServer().getConsoleSender().log(address + " disconnected the login server");
    }

    private void send(String message)
    {
        try
        {
            out.writeUTF(message);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * 로그인이나 회원가입 관련 패킷을 보낼때 정규화된 방식로 보내줍니다.
     */
    private void sendRegisterSocket(int status, String message)
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", status);
        jsonObject.addProperty("requestIp", address.getHostName());
        jsonObject.addProperty("message", message);
        this.send(jsonObject.toString());
    }

    private <T> T getFramework(String jsonMessage, Class<T> type)
    {
        Gson gson = new Gson();
        return gson.fromJson(jsonMessage, type);
    }


    private Task<Void> taskBackground = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
            try
            {
                while(in != null)
                {
                    String jsonReceivedMessage = in.readUTF();
                    // EOF 에러 시 해당 클라이언트는 꺼진 상태입니다.
                    try
                    {
                        // 요청받은 메세지를 JsonObject로 가져옵니다.
                        JsonObject jo = getFramework(jsonReceivedMessage, JsonObject.class);
                        int connectionNumber = jo.get("status").getAsInt();

                        // 로그인을 요청합니다.
                        if(connectionNumber == ServerProperty.REQUEST_LOGIN)
                        {
                            // REQUEST LOGIN은 포맷 스트림의 문제로 String 단위가 아니라 Packet 단위로 통째로 변환되었습니다.
                            // 이 패킷에만 해당됩니다.
                            LoginConnectionPacket loginConnection = getFramework(jsonReceivedMessage, LoginConnectionPacket.class);
                            String id = loginConnection.getId();

                            GameServer.getServer().getConsoleSender().log("Received the requesting: REQUEST_LOGIN_SIGNAL=[" + id + "]:" + address.getHostName());
                            if(new File("data/player/" + id + ".json").exists())
                            {

                                BufferedReader reader = new BufferedReader(new FileReader(new File("data/player/" + id + ".json")));
                                String jsonMessage = reader.readLine();

                                Player player = getFramework(jsonMessage, PlayerFramework.class);

                                if(player.getPassword().equalsIgnoreCase(SecurityUtil.Companion.sha256(loginConnection.getPassword())))
                                    sendRegisterSocket(ServerProperty.RECEVIED_LOGIN_SUCCESS, "로그인에 성공하였습니다.");
                                else
                                    sendRegisterSocket(ServerProperty.RECEIVED_LOGIN_FAILED, "비밀번호가 틀립니다. 비밀번호를 분실했을 경우 그냥 포기하세요.\n찾는 기능 같은거 없습니다 :D");
                            }
                            else
                            {
                                sendRegisterSocket(ServerProperty.RECEIVED_LOGIN_FAILED, "존재하지 않는 아이디입니다.");
                            }
                        }

                        // 로그인이 성공했을 경우입니다. 이것은 클라이언트에서 업데이트 서버와 연결할 준비를 한다는 것과 같습니다.
                        else if(connectionNumber == ServerProperty.RECEVIED_LOGIN_SUCCESS)
                        {
                            JsonObject jo2 = getFramework(jsonReceivedMessage, JsonObject.class);
                            GameServer.getConsoleSender().sendMessage(jo2.get("message").toString());
                        }

                        // 회원가입 절차를 검증합니다.
                        else if(connectionNumber == ServerProperty.CHECK_REGISTER_CONNECTION)
                        {
                            RegisterConnectionPacket registerConnection = getFramework(jsonReceivedMessage, RegisterConnectionPacket.class);
                            String id = registerConnection.getId();
                            GameServer.getServer().getConsoleSender().sendMessage("Player requested: CHECK_REGISTER_CONNECTION=[" + id + "]");
                            if(new File("data/player/" + id + ".json").exists())
                            {
                                JsonObject error = new JsonObject();
                                error.addProperty("status",ServerProperty.REGISTER_FAILED_ACCOUNT);
                                error.addProperty("message","해당 아이디는 이미 가입되어 있습니다.");
                                send(error.toString());
                            }
                            else
                            {
                                Player newPlayer = PlayerFramework.register(registerConnection.getId(), registerConnection.getNickname(), registerConnection.getPassword());
                                GameServer.getConsoleSender().log("Generating about information: " + newPlayer.toString());
                                JsonObject success = new JsonObject();
                                success.addProperty("status", ServerProperty.REGISTER_SUCCESSED_ACCOUNT);
                                success.addProperty("message", "회원가입에 성공하였습니다! 이제 로그인하시면 됩니다.\n아아디: " + newPlayer.getId());
                                send(success.toString());
                            }
                        }

                        // 클라이언트에서 파일 목록을 요청받고 서버 내 업데이트 파일의 대한 정보를 보냅니다.
                        // 이때, UpdateConnectionReceiver로 보내어 업데이트 포트로 정보를 보냅니다.
                        else if(connectionNumber == ServerProperty.SEND_UPDATE_REQURST)
                        {
                            Gson gsonSerialize = new Gson();
                            JsonObject object = new JsonObject();

                            object.addProperty("status", ServerProperty.SEND_UPDATE_REQURST_RECEIVED);
                            object.addProperty("data", SystemUtil.Companion.fixHashMap(gsonSerialize.toJson(Update.getUpdateFiles())));

                            // UpdateConnectionReceiver에게 보내줍니다.
                            DataOutputStream dos = ConnectionBackground.getUpdateClientMap().get(address.getHostAddress());
                            dos.writeUTF(object.toString());
                        }

                        // 업데이트가 모두 끝나고 게임 서버에 연결합니다.
                        else if(connectionNumber == ServerProperty.CONNECT_GAME_SERVER)
                        {
                            String id = jo.get("id").getAsString();
                            PlayerFramework framework = (PlayerFramework)GameServer.getServer().getPlayer(id);
                            ConnectionBackground.getGameClientMap().put((Player)framework, socket);
                        }

                        // 서버 시간을 클라이언트에 보냅니다.
                        else if(connectionNumber == ServerProperty.SERVER_TIME_REQUEST)
                        {
                            JsonObject object = new JsonObject();
                            object.addProperty("status", ServerProperty.SERVER_TIME_REQUEST);
                            object.addProperty("time", GameServer.getServer().getDateFormat().format(new Date()));
                            send(object.toString());
                        }

                        // 어떤 클라이언트가 채팅 시스템을 이용해 메세지를 보낸 상태입니다.
                        // 그 보낸 메세지를 모두에게 보여줘야 합니다.
                        // 메세지를 모두에게 보냅니다. (사용자가 메세지를 전송했습니다.)
                        else if(connectionNumber == ServerProperty.PLAYER_MESSAGE_RECEIVED)
                        {
                            Gson playerGson = new GsonBuilder().serializeNulls().registerTypeAdapter(Player.class, new PlayerAdapter()).create();
                            Player player = playerGson.fromJson(jo.get("player"), PlayerFramework.class);

                            String who = player.getNickname();
                            String message = jo.get("message").getAsString();

                            String finalMessage = who + "(" + player.getId() + ") : " + message;

                            // 콘솔에 메세지를 남깁니다.
                            GameServer.getConsoleSender().sendMessage(finalMessage);

                            // 메세지를 모두에게 보냅니다.
                            GameServer.getConsoleSender().sendAll(finalMessage);
                        }

                        //
                        // 방을 생성할 때 그런 정보를 담고 있는 패킷을 받았을 때입니다.
                        else if(connectionNumber == ServerProperty.ROOM_CREATE)
                        {
                            Gson gson = new GsonBuilder().serializeNulls().registerTypeAdapter(Player.class, new PlayerAdapter()).create();
                            Player player = gson.fromJson(jo.get("player"), PlayerFramework.class);
                            String roomName = jo.get("roomName").getAsString();
                            RoomFramework room = (RoomFramework) RoomFramework.generate(player, roomName);
                            room.setCreateDate(GameServer.getServer().getDateFormat().format(new Date()));
                            ConnectionBackground.getRoomMap().put(player, room);
                        }

                        else
                        {
                            GameServer.getConsoleSender().sendMessage("The packet number \"" + connectionNumber + "\" is unknown. Is your customize packet?", MessageType.WARNING);
                        }
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        SystemUtil.Companion.alert("예외 오류 발생", "오류가 발생했습니다. 고쳐야 할텐데..", e.getMessage());
                    }
                }
            }
            catch(EOFException e)
            {
                leave(address);
            }
            catch(IOException e)
            {
                e.printStackTrace();
                leave(address);
            }
            return null;
        }
    };

    public synchronized void asyncStart()
    {
        ConnectionBackground.getClientMap().put(address.getHostAddress(), out);
        Thread thread = new Thread(this.taskBackground);
        thread.start();
    }
}
