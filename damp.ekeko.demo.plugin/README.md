# CSMR WCRE 2014 Tool Demo 

Please see the documentation at the top of the [csmrwcre2014.clj](https://github.com/cderoove/damp.ekeko/blob/master/damp.ekeko.demo.plugin/src/damp/ekeko/demo/csmrwcre2014.clj) file. 


# damp.ekeko.EkekoDemo

Demonstrator of using the [damp.ekeko.EkekoPlugin](https://github.com/cderoove/damp.ekeko/tree/master/EkekoPlugin) from within other Clojure or Java code (e.g., within an Eclipse plugin). 

Key is to group your code into a minimalistic Eclipse plugin that depends on the Ekeko plugin.
To this end, ensure its MANIFEST.MF includes:

```
Require-Bundle: damp.ekeko.plugin
```

As [explained succintly on StackOverflow](http://stackoverflow.com/questions/8018207/clojure-classpath-issue-within-an-eclipse-plugin), also ensure the MANIFEST.MF has an:

```
Eclipse-RegisterBuddy: damp.ekeko.plugin
```
Otherwise, you won't be able to load the Ekeko namespaces.

The actual demonstrator depends on Ekeko to continuously identify and mark instances of the following code:
```Java

    public List<Integer> getChildren() {
    	return null;
    }     
```  
  that are candidates to be refactored to:

```Java

    public List<Integer> getChildren() {
    	return new ArrayList<Integer>();
    }     
```

The marking starts as soon as you have added a listener for Ekeko model changes along the following lines (e.g., from an Ekeko-hosted REPL):

```clj
  (use 'damp.ekeko.demo.markers)
  (in-ns 'damp.ekeko.demo.markers)
  
  (ekekomodel/register-listener 
    (model-update-listener 
      (incremental-node-marker
        (fn []
          (map first 
            (damp.ekeko/ekeko [?m] 
              (fresh [?t]
                 (relations/method-always-returning-null-for-ctype ?m ?t))))))))
```  

Check out the [screen recording](http://www.youtube.com/watch?v=kSPjXnJ7S6s&feature=g-upl) to see the plugin in action. 

Will be expanded as Ekeko progresses.


See [README.md](https://github.com/cderoove/damp.ekeko/blob/master/EkekoPlugin/README.md) of [EclipsePlugin](https://github.com/cderoove/damp.ekeko/tree/master/EkekoPlugin) for more information.
