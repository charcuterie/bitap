package bitap;

public class Main {

	public static void main(String[] args) {
		Character[] alphabet = {'a', 'b', 'c', 'd', 'e'};
		Bitap b = new Bitap("abcd", "abcdeeeeeeabcdeeeeeeabcd", alphabet);
		System.out.println(b.wuManber(2));
	}
}