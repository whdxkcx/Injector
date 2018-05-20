package Demo;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import Injector.Injector;
import jdk.jfr.Name;

@Singleton
class Root{
	@Inject
	@Named("a")
	Node a;
	
	
	@Inject
	@Named("b")
	Node b;
	
	@Override
	public String toString() {
		return String.format("root(%s,%s)",a.name(),b.name());
	}
}

interface Node{
	String name();
}

@Singleton
@Named("a")
class NodeA implements Node{
	
	@Inject
	Leaf leaf;
	
	@Inject
	@Named("b")
	Node b;

	
	@Override
	public String name() {
		if(b==null)
			return String.format("nodeA(%s)", leaf);
		else
			return String.format("nodeAWithB(%s)", leaf);
	}
	
}


@Singleton
@Named("b")
class NodeB implements Node{

	Leaf leaf;
	
	@Inject
	@Named("a")
	Node  a;
	
	
	//¹¹ÔìÆ÷×¢Èë
	@Inject
	public NodeB(Leaf leaf) {
		this.leaf=leaf;
	}
	
	@Override
	public String name() {
		if(a==null) 
			return String.format("NodeB(%s)", leaf);
		else
			return String.format("NodeBWithA(%s)", leaf);
	}
	
}


class Leaf {
	@Inject
	Root root;
	
	int index;
	static int sequence;
	
	public Leaf() {
		index=sequence++;
	}
	
	@Override
	public String toString() {
		if(root==null)
			return "leaf"+index;
		else
			return "leafWithroot"+index;
	}
}
public class Demo {
    public static void main(String[] args) throws Exception
    {
    	var injector=new Injector();
    	injector.registerQualifiedClass(Node.class, NodeA.class);
    	injector.registerQualifiedClass(Node.class, NodeB.class);
    	var root=injector.getInstance(Root.class);
    	System.out.println(root);
    }
	
}
