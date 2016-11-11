# allocation-assistant
resource allocation assistance for recurring parallel dataflow jobs

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
