# FilePlotter
Uses a specified file to plot data. The goal of this applications is to allow users to generate plots with simple files without needing to 
create any intricate file formatting; all that is required for this plotting to work is that there is an even number of data points and
that each number entry is separated by whitespace.

### Usage

Java required. Compile `Plotter.java` and run `java Plotter INPUT_FILE` to plot the specified file.

### A quick note about the format of input files

The input files must have all the points in `X Y` order with a space separating all data points.
All data must appear in pairs; an odd number of data points will cause an error to be generated.

The program also accepts an optional format specifier in the following formats:

color can be specified with : `{R,G,B}`
where `R`, `G`, `B` are integer values between 0-255 representing red, green and blue, respectively

size can be specified with : `[S]`
where `S` is a positive integer. (behavior unspecified with negative values)

Default color is black, and default size is 7.

Note that these formatting options must appear after the Y coordinate of the point to modify, and there must be
whitespace between each specifier and between the points and the specifiers. If both specifiers are used, the color specifier MUST be before the size specifier.

**Sample input files are available to see and try at https://github.com/maxwey/FilePlotter/tree/master/examples**



### History and future

This application was created as a quick side project when I wanted to be able to quickly plot generated data. 
This project may or may not be continued to be updated; possible additions include: 
- a format specifier (that way any format can be parsed once it has been identified)
- simple data viewing tools (eg tooltips showing more information about a selected point, or creating lines between points)
