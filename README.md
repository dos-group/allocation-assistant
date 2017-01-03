# allocation-assistant

Automatic resource allocation for recurring distributed dataflow jobs given a user-defined runtime target.

Given, for example, a recurring Spark job that runs SGD (mllib) to find parameters for a training dataset of 10 GB (20000000 data points, each with 20 features) using 100 iterations and a step size of 1.0, using the allocation-assistant (d041bde, Dec 8, 2016) to allocate resources for a target runtime of 800 seconds resulted in the following allocations and runtimes:

![Example of a recurring Spark job implementing SGD](doc/example_Dec08_2016.png?raw=true)

## Compiling
install Freamon (https://github.com/citlab/freamon):

	git clone <freamon url>
	cd freamon
	mvn install

compile allocation-assistant:

	cd ..       # where you cloned allocation-assistant
	mvn package

## Running
1. setup+start freamon (see freamon readme)
1. create your own config based on `doc/cluster.conf`
1. `./allocation-assistent <your args>`

To see available arguments run `./allocation-assistent --help`
