# damp.ekeko.EkekoPlugin

Ekeko enables querying and manipulating an Eclipse workspace using applicative logic programs.

Its libraries provide support for answering program queries (e.g., "*is my code bug free?*" or "*does my code follow the prescribed design?*") as well as transforming programs (e.g., "*patch my code as follows*") in a declarative manner.

Ekeko is based on the excellent [core.logic](https://github.com/clojure/core.logic) port to [Clojure](http://clojure.org/) of the applicative logic programming library [Kanren](http://kanren.sourceforge.net/).

Ekeko is meant as the successor to the [SOUL logic program query language](http://soft.vub.ac.be/SOUL/), which still hosts academic papers about logic program querying.


## Installation

Install the prebuilt Ekeko plugin from the Eclipse update site: 
[http://soft.vub.ac.be/eclipse/update-3.7/](http://soft.vub.ac.be/eclipse/update-3.7/) 

See [Installing New Software](http://help.eclipse.org/juno/topic/org.eclipse.platform.doc.user/tasks/tasks-124.htm) for help on installing Eclipse plugins from an update site. 

Ekeko has been tested against [Eclipse Juno (4.2)](http://www.eclipse.org)

Plugin dependencies [org.eclipse.jdt.astview](http://www.eclipse.org/jdt/ui/astview/index.php) and [Counterclockwise](http://code.google.com/p/counterclockwise/) should be installed automatically.

Alternatively, the plugin can also be built from [EclipsePlugin](https://github.com/cderoove/damp.ekeko/tree/master/EkekoPlugin) and its feature from [EclipseFeature](https://github.com/cderoove/damp.ekeko/tree/master/EkekoFeature) using the Eclipse export wizards on the [plugin.xml](https://github.com/cderoove/damp.ekeko/blob/master/EkekoPlugin/plugin.xml) and [feature.xml](https://github.com/cderoove/damp.ekeko/blob/master/EkekoFeature/feature.xml) files from the respective Eclipse projects. 

## Documentation

See the [damp.ekeko wiki](https://github.com/cderoove/damp.ekeko/wiki) for information on:  

* [how to get started](https://github.com/cderoove/damp.ekeko/wiki/Getting-Started-with-Ekeko)
* [example queries](https://github.com/cderoove/damp.ekeko/wiki/Example-Ekeko-Queries)

See the [damp.ekeko API documentation](http://cderoove.github.com/damp.ekeko/) for an overview of all relations and functions that are included.

## License  

Copyright © 2012 Ekeko contributors: 

* [Coen De Roover](http://soft.vub.ac.be/~cderoove/): initial design, implementation and documentation
 
* [Carlos Noguera](http://soft.vub.ac.be/soft/members/carlosnoguera): Barista graphical [user interface](http://soft.vub.ac.be/SOUL/home/querying-from-eclipse/running-and-inspecting-a-query/)

* [Reinout Stevens](http://soft.vub.ac.be/soft/members/reinoutstevens): damp.ekeko.workspace.reification 

Distributed under the Eclipse Public License.

Ekeko stands on the shoulders of open source giants.    
Included dependencies:

* Clojure's applicative logic programming library [core.logic](https://github.com/clojure/core.logic/) (Eclipse Public License)
* [Reinout Stevens](http://soft.vub.ac.be/soft/members/reinoutstevens)' regular path expression library [damp.qwal](https://github.com/ReinoutStevens/damp.qwal 
) (Eclipse Public License)
* Anders Hessellund's intra-procedural JDT control flow graph [dk.itu.smartemf.ofbiz.analysis.ControlFlowGraph](http://www.itu.dk/people/hessellund/smartemf/index.php
) (Apache license)
* Sable's Java Optimization Framework [Soot](http://www.sable.mcgill.ca/soot/
) (LGPL)

External dependencies:

* Eclipse plugin [org.eclipse.jdt.astview](http://www.eclipse.org/jdt/ui/astview/index.php) (Eclipse Public License)
* Eclipse plugin [Counterclockwise](http://code.google.com/p/counterclockwise/
) (Eclipse Public License) 