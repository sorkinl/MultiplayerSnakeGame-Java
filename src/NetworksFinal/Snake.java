package NetworksFinal;


public class Snake {
	/**
	 * 
	 */
	private String snake;
	private int score = 0;

	public Snake(String thisSnake) {
		snake = thisSnake;
	}

	
	public int getScore() {
		return score;
	}
	
	public void setScore(int newScore) {
		score = newScore;
	}
	
	public void incrementByTen() {
		score += 10;
	}

	public void setSnake(String newSnake) {
		snake = newSnake;
	}
	
	
	public String getSnake() {

		return snake;
	}
	
	public int getHeadX() {
		int x = Integer.parseInt(snake.substring(snake.indexOf('"' + "x" + '"' + ":") + 4,
				snake.indexOf(",")));
		return x;
	}
	public int getHeadY() {
		int y = Integer.parseInt(snake.substring(snake.indexOf('"' + "y" + '"' + ":") + 4,
				snake.indexOf("}")));
		return y;
	}
	
	public String getFullSnake() {
		return new String("{" + '"' + "score" + '"' + ":" + getScore() + "," + '"'
				+ "snake" + '"' + ":" + getSnake() + "}");
	}

}
