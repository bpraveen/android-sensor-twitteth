/*
 * 
 * Copyright (C) 2010 The Android Open Source Project 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 */

package tweet.sensor;

/*
 * Class for different features of measurement in every User Activity
 * like average, standard deviation, peek to peek
 * 
 * Provides default values for every activity's feature 
 * 
 * @author Ebby Wiselyn	<ebbywiselyn@gmail.com>
 * @author PraveenKumar Bhadrapur	<praveenkumar.bhadarpur@gmail.com>
 * @version 0.0.1
 * 
 */
public class UserInfoActivityFeature {
	public enum Name {
		ACTIVITY_STAND, ACTIVITY_WALK, ACTIVITY_RUN
	};

	// Walk Standard Deviation values
	public static double walkStdDev = 2.314436111629097;
	public static double walkMaxStdDev;
	public static double walkMinStdDev;

	// Walk Peek to Peek values
	public static double walkPeekToPeek = 32.28362518120852;
	public static double walkMinPeekToPeek;
	public static double walkMaxPeekToPeek;

	// Walk Average values
	public static double walkAvg = 11.295184752921534;
	public static double walkMinAvg;
	public static double walkMaxAvg;

	// Run Standard Deviation Values
	public static double runStdDev = 2.1459532068717206;
	public static double runMaxStdDev;
	public static double runMinStdDev;

	// Run Peek to Peek Values
	public static double runPeekToPeek = 33.80161096617945;
	public static double runMinPeekToPeek;
	public static double runMaxPeekToPeek;

	// Run Average Values
	public static double runAvg = 11.879737513671376;
	public static double runMinAvg;
	public static double runMaxAvg;

	// Stand Standard Deviation
	public static double standStdDev = 1.0472167012925262;
	public static double standMaxStdDev;
	public static double standMinStdDev;

	// Stand Peek to Peek
	public static double standPeekToPeek = 28.1736325226226;
	public static double standMinPeekToPeek;
	public static double standMaxPeekToPeek;

	// Stand Average
	public static double standAvg = 10.007368031875503;
	public static double standMinAvg;
	public static double standMaxAvg;

	/*
	 * Return the String description of the activity Name
	 * 
	 * @param Name Enumeration value of Activity
	 * 
	 * @return String Returns the String description of the Activity
	 */
	public static String getName(Name a) {

		if (a == Name.ACTIVITY_RUN) {
			return "Run";
		} else if (a == Name.ACTIVITY_STAND) {
			return "Stand";
		} else {
			return "Walk";
		}
	}
}
