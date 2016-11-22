package me.ziry.client;

import java.awt.BorderLayout;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class ChatClient extends JFrame {

	/*
	 * Chat0.5修改了List<Socket> sList 
	 * 修复了服务器关闭的bug 和修复了客户端的bug 修复逻辑错误
	 */
	private static final long serialVersionUID = 1L;

	TextField tf1 = new TextField(); // 输入框
	TextArea ta1 = new TextArea(); // 显示框
	Socket s = null;
	DataOutputStream dos = null;

	public static void main(String[] args) {
		new ChatClient();
	}

	/**
	 * 
	 * 获取指定区间大小的随机数
	 * 
	 * @param min
	 *            最小值
	 * @param max
	 *            最大值
	 * @return
	 */
	public int getRandom(int min, int max) {
		return (int) (Math.random() * (max - min) + min);
	}

	// 得到用户昵称及服务器信息
	public String[] showSetServer() {

		JTextField tf1 = new JTextField(10); // 对话框输入文本，昵称。
		tf1.setText(getRandom(1000, 9999) + "");
		JTextField tf2 = new JTextField(16); // 对话框输入文本，服务器地址。
		tf2.setText("127.0.0.1");
		JTextField tf3 = new JTextField(5); // 对话框输入文本，端口号。
		tf3.setText("8888");

		String[] data = new String[3];
		boolean isData = true; // 控制用户循环输入

		while (isData) {

			int r = JOptionPane.showConfirmDialog(null, new Object[] { "您的昵称：", tf1, "服务器地址: ", tf2, "端口号: ", tf3 }, "连接服务器",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			if (r == JOptionPane.OK_OPTION) {

				data[0] = tf1.getText(); // 得到昵称。
				data[1] = tf2.getText().trim(); // 得到服务器地址。
				data[2] = tf3.getText().trim(); // 得到端口号。

				try {
					// 简单的判断输入是否有误
					if (data[1].length() > 6 && data[2].length() < 6) {

						if (Integer.parseInt(data[2]) < 65535 && Integer.parseInt(data[2]) > 1024) {

							isData = false;

						} else {

							JOptionPane.showMessageDialog(null, "输入有误，请重新输入！", "输入有误", JOptionPane.ERROR_MESSAGE);

						}

					} else {

						JOptionPane.showMessageDialog(null, "输入有误，请重新输入！", "输入有误", JOptionPane.ERROR_MESSAGE);

					}

				} catch (Exception e) {

					JOptionPane.showMessageDialog(null, "输入有误，请重新输入！", "输入有误", JOptionPane.ERROR_MESSAGE);

				}

			} else {
				System.exit(0);
			}
		}

		return data;

	}

	// 绘制窗口
	public ChatClient() {

		String[] data = showSetServer();
		linkServer(data[0], data[1], Integer.parseInt(data[2])); // 连接服务器自定义方法

		this.setTitle(data[0]); // 设置标题
		ta1.setEditable(false); // 显示框补课编辑
		this.setLocation(350, 200); // 设置窗体位置
		this.add(tf1, BorderLayout.SOUTH); // 添加输入框
		this.add(ta1, BorderLayout.CENTER); // 添加显示框
		tf1.addActionListener(new Tf1Listener()); // 输入框增加监听器
		this.addWindowListener(new CloseClient()); // 监听处理窗口关闭事件
		this.pack(); // 自动匹配窗体大小
		this.setVisible(true);

	}

	private class Tf1Listener implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			String tempStr = tf1.getText().trim(); // 得到输入框输入的字符串
			// ta1.append(tempStr+"\n");
			tf1.setText(""); // 清空输入框

			try {
				// 判断是否已连接服务器
				if (s != null) {

					dos = new DataOutputStream(s.getOutputStream());
					dos.writeUTF(getTitle() + ":\n    " + tempStr + "\n");
					dos.flush();
					// System.out.println("写给服务器");
				} else {

					ta1.append("服务器已关闭，详情请咨询lee@ziry.me\n");
					closeIO(); // 关闭连接以免循环输入导致异常

				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}
	}

	// 连接服务器
	public void linkServer(String name, String ip, int dk) {

		try {

			s = new Socket(ip, dk); // 连接服务器
			dos = new DataOutputStream(s.getOutputStream());
			dos.writeUTF(name); // 写入昵称用于服务器记录
			dos.flush();
			// System.out.println("NAME = "+name);

		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "输入有误，请重新运行本程序", "输入有误", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		putServer ps = new putServer(s); // 连接成功，单独线程读服务器数据。
		Thread t = new Thread(ps);
		t.start();

	}

	// 关闭Socket和dos流
	public void closeIO() {

		try {

			if (dos != null) {

				dos.close();

			}

			if (s != null) {

				s.close();
				s = null;

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// 处理窗口关闭事件
	class CloseClient extends WindowAdapter {

		public void windowClosing(WindowEvent e) {

			try {

				if (s != null) {
					// 写入关闭代码
					dos.writeUTF("01010101200203030303f!^DSFsdfggDFSgASDFasdf%^*&*&65@#$!123@#!#@!#123123156321");
					dos.writeUTF("" + s.getLocalAddress()); // 写入本IP地址
					dos.writeUTF(getTitle()); // 写入昵称

				}

			} catch (IOException e1) {

				e1.printStackTrace();

			}

			closeIO();
			System.exit(0);

		}

	}

	// 单独线程读服务器数据。
	class putServer implements Runnable {

		private Socket s = null;
		private DataInputStream dis = null;
		private boolean readServer = false;

		putServer(Socket s) {
			this.s = s;
		}

		public void run() {

			String str; // 用于保存服务器读来信息

			try {

				dis = new DataInputStream(s.getInputStream());
				readServer = true; // 得到流则开始读数据

				while (readServer) {

					try {

						str = dis.readUTF();
						ta1.append(str + "\n"); // 显示在显示框

						if (str.equals("服务器已退出！详情请咨询lee@ziry.me")) {
							closeIO(); // 关闭Socket和dos流
							readServer = false; // 停止读数据
						}

					} catch (IOException e) {

						readServer = false;
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
						s = null;
					}
				} catch (IOException e) {

					e.printStackTrace();

				}

			}

		}

	}

}
