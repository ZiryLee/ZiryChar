package me.ziry.server;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/*
 * Chat0.6优化Chat0.5版本 各项功能基本完全 
 * 完成时间2012.10.13 15：00 
 * 待完 成
 *  1、客户端查看在线用户
 *  2、私聊
 * 修复逻辑错误
 */
public class ChatServer extends JFrame {

	private static final long serialVersionUID = 1L;

	public static int PORT = 8888; // 端口号
	ServerSocket ss = null;
	Socket s = null;
	boolean bAccpetClient = false; // 控制是否循环接受到此套接字的连接
	List<Socket> sList = new ArrayList<Socket>(); // 保存个ClientSocket，用于同步输出

	// 二维表记录客户端信息
	String[] columnNames = { "客户端名称", "客户端IP地址", "端口号", "状态" };
	Object[][] rowData = {};
	DefaultTableModel defaultModel = new DefaultTableModel(rowData, columnNames);
	JTable table = new JTable(defaultModel);
	JScrollPane JSPanel = new JScrollPane(table);
	JPanel SPanel = new JPanel();

	// 绘制窗口
	public ChatServer() {

		this.setTitle("ChatSrever-0.5");
		this.setBounds(50, 50, 300, 600);
		this.setResizable(false); // 窗口不可编辑
		SPanel.add(JSPanel);
		this.add(SPanel, BorderLayout.SOUTH);
		this.pack();
		this.addWindowListener(new CloseSrever());
		this.setVisible(true);

	}

	// 主线程main方法
	public static void main(String[] args) {

		new ChatServer().acceptClient();

	}

	// 得到Client名称
	private String getClientName(DataInputStream dis) {

		String str = null;

		try {

			str = dis.readUTF();

		} catch (IOException e) {

			e.printStackTrace();
			str = null;

		}

		return str;
	}

	// 启动服务器并监听PORT端口，循环接受到此套接字的连接。
	public void acceptClient() {

		try {

			ss = new ServerSocket(PORT); // 启动服务器并监听PORT端口
			bAccpetClient = true; // 启动循环接受到此套接字的连接。

		} catch (IOException e1) {

			JOptionPane.showMessageDialog(null, "端口已被使用！请关掉相关程序并重新运行！", "警告", JOptionPane.ERROR_MESSAGE);
			System.exit(0);

		}

		try {

			while (bAccpetClient) {

				s = ss.accept(); // 侦听并接受到此套接字的连接。

				if (bAccpetClient) {

					WorkClient wc = new WorkClient(s); // new一个单独的线程类处理此套接字
					sList.add(s); // 保存此套接字以便同步其他客户端
					Thread t = new Thread(wc);
					t.start(); // 启动单独的线程类处理此套接字

				}
			}

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (s != null) {
					s.close();
				}

				if (ss != null) {
					ss.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	// 关闭ServerSocket与Socket
	public void closeSrever() {

		bAccpetClient = false; // 让循环侦听套接字停止

		try {

			if (s != null) {
				s.close();
			}

			if (ss != null) {
				new Socket("127.0.0.1", PORT).close(); // 防止accept()方法突然被打断抛出异常
				ss.close();
			}

		} catch (IOException e) {

			e.printStackTrace();

		}
	}

	// 循环向个cliten端发送数据
	public void send(String str) {

		DataOutputStream dos = null;
		Iterator<Socket> i = sList.iterator();

		while (i.hasNext()) {

			Socket s = i.next();

			try {

				if (s.isClosed()) { // 如果套接字已关闭则remove此套接字

					i.remove();

				} else {

					dos = new DataOutputStream(s.getOutputStream());
					dos.writeUTF(str);
					dos.flush();

				}

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

	}

	// 线程类 用来单独处理每个连接的cliten
	class WorkClient implements Runnable {

		private Socket s = null;
		private DataInputStream dis = null;
		private boolean readClient = false; // 用来控制循环读Client端

		WorkClient(Socket s) {
			this.s = s;
		}

		public void run() {

			String[] str = new String[3];

			try {

				dis = new DataInputStream(s.getInputStream());

				Object[] o = { getClientName(dis), "" + s.getInetAddress(), "" + s.getPort(), s.isConnected() };
				defaultModel.addRow(o); // 将client端信息放到JTable模版中

				readClient = true;

				while (readClient) {

					try {

						// System.out.println("Server正在读数据");
						str[0] = dis.readUTF(); // 读入客户端输入并判断是否为关闭代码
						// System.out.println("读入客户端信息"+str[0]);
						if (str[0].equals("01010101200203030303f!^DSFsdfggDFSgASDFasdf%^*&*&65@#$!123@#!#@!#123123156321")) {

							str[1] = dis.readUTF(); // 读客户端Ip
							str[2] = dis.readUTF(); // 读客户端名称

							for (int i = 0; i < defaultModel.getRowCount(); i++) {

								if (str[1].equals((String) defaultModel.getValueAt(i, 1))) {

									if (str[2].equals((String) defaultModel.getValueAt(i, 0))) {

										defaultModel.setValueAt(new Date(), i, 3); // 将客户端状态给为退出出时间
										str[0] = str[2] + "已退出，by-by~"; // 通知客户端我已退出
										break;

									}

								}

							}

						}

						send(str[0]); // 循环向个cliten端发送数据

					} catch (Exception e) {

						readClient = false;
						e.getStackTrace();

					}

				}

			} catch (IOException e) {

				e.printStackTrace();

			} finally {

				try {

					if (dis != null) {
						dis.close();
					}

					if (s != null) {
						s.close();
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}
	}

	// 监听类 处理屏幕关闭
	class CloseSrever extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			send("服务器已退出！详情请咨询lee@ziry.me"); // 通知各客户端
			closeSrever(); // 关闭各各流
			System.exit(0);
		}
	}
}
