package NetworksFinal;

import java.util.Random;

public class Food {
	
	
	private int x;
	private int y;
	
	
	public Food() {
		this.x = random_pos(39);
		this.y = random_pos(39);
	}
	
	
	public int random_pos(int max) {
		Random rnd = new Random();
		int num = (rnd.nextInt(max) + 1) * 10;
		return num;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public void genRandomFood() {
		x = random_pos(39);
		y = random_pos(39);
	}
	
	

}
