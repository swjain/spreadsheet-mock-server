import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class CarFactory {
	List<Car> inventory;
	List<String> safeColours = List.of("Blue", "White");

	public Predicate<Car> getSafetyChecker() {
		return x -> safeColours.contains(x.Colour());
	}

	public static void main(String[] args) {
		CarFactory factory = new CarFactory();
		factory.inventory = List.of(
			new Car("Blue"),
			new Car("Red"),
			new Car("White")
		);

		Predicate<Car> safetyChecker = factory.getSafetyChecker();
		List<Car> safeCars = factory.inventory.stream().filter(safetyChecker).collect(Collectors.toList());
		System.out.println(safeCars);
	}

}

class Car {
    String colour;
    public Car(String colour) {
        this.colour = colour;
    }

    public String Colour() {
        return colour;
    }

    @Override
    public String toString() {
        return colour;
    }
}