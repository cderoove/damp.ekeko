package damp.ekeko;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchyChangedListener;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.google.common.collect.Iterables;

import edu.cmu.cs.crystal.cfg.IControlFlowGraph;
import edu.cmu.cs.crystal.cfg.eclipse.EclipseCFG;


public class JavaProjectModel extends ProjectModel implements ITypeHierarchyChangedListener {
	
	protected IJavaProject javaProject;
	
	protected ConcurrentHashMap<ICompilationUnit,CompilationUnit> icu2ast;
	
	public ConcurrentHashMap<String,TypeDeclaration> typeDeclarations;
	private Set<TypeDeclaration> typeDeclarationsWithoutBinding;

	private ConcurrentHashMap<String,AnnotationTypeDeclaration> annotationTypeDeclarations;
	private Set<AnnotationTypeDeclaration> annotationTypeDeclarationsWithoutBinding;

	private ConcurrentHashMap<String,EnumDeclaration> enumDeclarations;
	private Set<EnumDeclaration> enumDeclarationsWithoutBinding;
	
	private ConcurrentHashMap<String,AnonymousClassDeclaration> anonymousClassDeclarations;
	private Set<AnonymousClassDeclaration> anonymousClassDeclarationsWithoutBinding;
	
	private ConcurrentHashMap<String,MethodDeclaration> methodDeclarations;
	private Set<MethodDeclaration> methodDeclarationsWithoutBinding;
	
	private Set<FieldDeclaration> fieldDeclarations;

	private Set<SingleVariableDeclaration> singleVariableDeclarations;

	private Set<EnumConstantDeclaration> enumConstantDeclarations;

	private Set<AnnotationTypeMemberDeclaration> annotationTypeMemberDeclarations;
	
	//TODO: if necessary, cache the bindings they resolve to here
	private Set<Type> types;

	private Set<ASTNode> fieldAccessLikeNodes;
	
	private Set<ASTNode> invocationLikeNodes;
	
	private ConcurrentHashMap<MethodDeclaration,IControlFlowGraph<ASTNode>> controlFlowGraphs;
	
	private Set<Statement> statements;	
	private Set<Expression> expressions;
	private Set<ASTNode> nodes;

	private boolean ignoreCompilationErrors = false;
	
	
	private ConcurrentHashMap<IType,ITypeHierarchy> itype2typehierarchy;

	//private ConcurrentHashMap<MethodDeclaration, Set<MethodDeclaration>> methodOverriders;

	//private ConcurrentHashMap<MethodDeclaration, Set<MethodDeclaration>> methodOverridden;
	
	public JavaProjectModel(IProject p) {
		super(p);
		javaProject = JavaCore.create(p);
		clean();
	}

	public IJavaProject getJavaProject() {
		return javaProject;
	}
	
	public Iterable<ICompilationUnit> getICompilationUnits() {
		return icu2ast.keySet();
	}
	
	public CompilationUnit getCompilationUnit(ICompilationUnit icu) {
		return icu2ast.get(icu);
	}
	
	public Iterable<CompilationUnit> getCompilationUnits() {
		return icu2ast.values();
	}
	
	public Iterable<Expression> getExpressions() {
		return expressions;
	}
	
	public Iterable<Statement> getStatements() {
		return statements;
	}	
	
	public Iterable<Type> getTypes() {
		return types;
	}	
	
	//SimpleName, QualifiedName, FieldAccess, SuperFieldAccess
	public Iterable<ASTNode> getFieldAccessLikeNodes() {
		return fieldAccessLikeNodes;
	}	

	//MethodInvocation, SuperMethodInvocation, ClassInstanceCreation, ConstructorInvocation, SuperConstructorInvocation
	public Iterable<ASTNode> getInvocationLikeNodes() {
		return invocationLikeNodes;
	}	
	
	public Iterable<FieldDeclaration> getFieldDeclarations() {
		return fieldDeclarations;
	}
	
	public Iterable<SingleVariableDeclaration> getSingleVariableDeclarations() {
		return singleVariableDeclarations;
	}
	
	public Iterable<EnumConstantDeclaration> getEnumConstantDeclarations() {
		return enumConstantDeclarations;
	}
	
	public Iterable<AnnotationTypeMemberDeclaration> getAnnotationTypeMemberDeclarations() {
		return annotationTypeMemberDeclarations;
	}

	public Iterable<TypeDeclaration> getTypeDeclarations() {
		return Iterables.concat(typeDeclarations.values(), typeDeclarationsWithoutBinding);
	}

	public Iterable<MethodDeclaration> getMethodDeclarations() {
		return Iterables.concat(methodDeclarations.values(), methodDeclarationsWithoutBinding);
	}
	
	public Iterable<EnumDeclaration> getEnumDeclarations() {
		return Iterables.concat(enumDeclarations.values(),enumDeclarationsWithoutBinding);
	}
	
	public Iterable<AnnotationTypeDeclaration> getAnnotationTypeDeclarations() {
		return Iterables.concat(annotationTypeDeclarations.values(),annotationTypeDeclarationsWithoutBinding);
	}
	
	public Iterable<AnonymousClassDeclaration> getAnonymousClassDeclarations() {
		return Iterables.concat(anonymousClassDeclarations.values(),anonymousClassDeclarationsWithoutBinding);
	}
	
	public IControlFlowGraph<ASTNode> getControlFlowGraph(MethodDeclaration m) {
		IControlFlowGraph<ASTNode> graph = controlFlowGraphs.get(m);
		if(graph == null) {
			graph = newControlFlowGraph(m);
			controlFlowGraphs.putIfAbsent(m, graph);
		}
		return graph;
	}
	
	public ITypeHierarchy getTypeHierarchy(IType type) throws JavaModelException {
		ITypeHierarchy typeHierarchy;
		if (!itype2typehierarchy.containsKey(type)) {
			typeHierarchy = type.newTypeHierarchy(new NullProgressMonitor());
			addTypeHierarchy(typeHierarchy);
		}
		else
			typeHierarchy = itype2typehierarchy.get(type);
		return typeHierarchy;
	}
	

	
	public boolean retrieveIgnoreCompilationErrors() {
		String args = null;
		try {
			args = getProject().getPersistentProperty(EkekoProjectPropertyPage.PROCESSERRORS_PROPERTY);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return Boolean.parseBoolean(args);
	}
	
	

	//no longer called, but kept for the bug information
	public CompilationUnit[] parse(ICompilationUnit[] icus, IProgressMonitor monitor) {
		final CompilationUnit[] compilationUnits = new CompilationUnit[icus.length];
		
		//Normally, would parse entire batch of ICompilationUnits
		//But there is an annoying known bug:
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=359478
		//http://stackoverflow.com/questions/7603096/why-am-i-getting-nullpointerexception-in-the-compilationunit-instances-returned
		//parser.createASTs(icus, new String[0], requestor, monitor); 
		//The following is a lot slower, but seems to work fine
		//TODO: upon new JDT release, check whether bug has been resolved
		int i = 0;
		for(ICompilationUnit icu : icus) 
		    compilationUnits[i++] = parse(icu, monitor);
		return compilationUnits;
	}
	
	
	public static int JLS = AST.JLS8;
		
	public static CompilationUnit parse(ICompilationUnit icu, IProgressMonitor monitor) {
		//Removing the next three calls to parser results in null-bindings (even though already set in constructor)
		ASTParser parser = ASTParser.newParser(JLS); //seems better than reusing the existing one (ran out of memory on azureus otherwise)			
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(icu.getJavaProject());
		parser.setStatementsRecovery(false);
		parser.setBindingsRecovery(false);
		parser.setSource(icu);
		return (CompilationUnit) parser.createAST(monitor);
	}
	
		

	public void clean() {
		super.clean();
		icu2ast = new ConcurrentHashMap<ICompilationUnit, CompilationUnit>();
		
		typeDeclarations = new ConcurrentHashMap<String,TypeDeclaration>();
		typeDeclarationsWithoutBinding = java.util.Collections.newSetFromMap(new ConcurrentHashMap<TypeDeclaration,Boolean>());
		
		methodDeclarations = new ConcurrentHashMap<String,MethodDeclaration>();
		methodDeclarationsWithoutBinding = java.util.Collections.newSetFromMap(new ConcurrentHashMap<MethodDeclaration,Boolean>());

		//field declarations don't have a binding, their VariableDeclarationFragments do
		fieldDeclarations = java.util.Collections.newSetFromMap(new ConcurrentHashMap<FieldDeclaration,Boolean>());
		
		singleVariableDeclarations = java.util.Collections.newSetFromMap(new ConcurrentHashMap<SingleVariableDeclaration,Boolean>());
		
		enumConstantDeclarations = java.util.Collections.newSetFromMap(new ConcurrentHashMap<EnumConstantDeclaration,Boolean>());

		annotationTypeMemberDeclarations = java.util.Collections.newSetFromMap(new ConcurrentHashMap<AnnotationTypeMemberDeclaration,Boolean>());

	
		enumDeclarations = new ConcurrentHashMap<String,EnumDeclaration>();
		enumDeclarationsWithoutBinding = java.util.Collections.newSetFromMap(new ConcurrentHashMap<EnumDeclaration,Boolean>());
	
		annotationTypeDeclarations = new ConcurrentHashMap<String,AnnotationTypeDeclaration>();
		annotationTypeDeclarationsWithoutBinding = java.util.Collections.newSetFromMap(new ConcurrentHashMap<AnnotationTypeDeclaration,Boolean>());

		anonymousClassDeclarations = new ConcurrentHashMap<String,AnonymousClassDeclaration>();
		anonymousClassDeclarationsWithoutBinding = java.util.Collections.newSetFromMap(new ConcurrentHashMap<AnonymousClassDeclaration,Boolean>());

		expressions = java.util.Collections.newSetFromMap(new ConcurrentHashMap<Expression,Boolean>());
		statements = java.util.Collections.newSetFromMap(new ConcurrentHashMap<Statement,Boolean>());
		
		nodes = java.util.Collections.newSetFromMap(new ConcurrentHashMap<ASTNode,Boolean>());
		

		
		itype2typehierarchy = new ConcurrentHashMap<IType, ITypeHierarchy>();
	
		controlFlowGraphs = new  ConcurrentHashMap<MethodDeclaration,IControlFlowGraph<ASTNode>>();
		
		types = java.util.Collections.newSetFromMap(new ConcurrentHashMap<Type,Boolean>());

		fieldAccessLikeNodes = java.util.Collections.newSetFromMap(new ConcurrentHashMap<ASTNode,Boolean>());
		invocationLikeNodes = java.util.Collections.newSetFromMap(new ConcurrentHashMap<ASTNode,Boolean>());
		
		ignoreCompilationErrors = retrieveIgnoreCompilationErrors();
		
		/*
		methodOverriders = new ConcurrentHashMap<MethodDeclaration, Set<MethodDeclaration>>();
		methodOverridden = new ConcurrentHashMap<MethodDeclaration, Set<MethodDeclaration>>();
		*/
	
	}
	
	
	public void populate(IProgressMonitor monitor) throws CoreException {
		super.populate(monitor);
		String msg = "Populating JavaProjectModel for: " + javaProject.getElementName();
		EkekoPlugin.getConsoleStream().println(msg);
		IPackageFragment[] packageFragments = javaProject.getPackageFragments();
	    SubMonitor sub = SubMonitor.convert(monitor, msg, packageFragments.length);
		for(IPackageFragment frag : packageFragments) {
			if(sub.isCanceled()) {
				buildCanceled();
				return;
			}
			parsePackageFragment(frag, sub);
			sub.worked(1);
		}	
		gatherInformationFromCompilationUnits();
	}
	
	
	public static Collection<IMarker> getCompilationErrors(ICompilationUnit icu) {
		LinkedList<IMarker> errors = new LinkedList<IMarker>();
		IResource resource = icu.getResource();	
		try {
			for (IMarker marker: resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER,true,IResource.DEPTH_INFINITE)) {
				Integer severityType = (Integer) marker.getAttribute(IMarker.SEVERITY);
				if (severityType.intValue() == IMarker.SEVERITY_ERROR)
					errors.add(marker);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return errors;
	}

	public static boolean compilationUnitHasCompilationErrors(ICompilationUnit icu) {
		return !getCompilationErrors(icu).isEmpty();
	}

	private void parsePackageFragment(IPackageFragment frag, IProgressMonitor monitor) throws JavaModelException {
		ICompilationUnit[] icus = frag.getCompilationUnits();
		SubMonitor sub = SubMonitor.convert(monitor, icus.length);
		for(ICompilationUnit icu : icus) {
			CompilationUnit cu = parseCompilationUnit(icu, sub);
			if(cu != null)
				icu2ast.put(icu, cu);
		}		
	}
	
	protected CompilationUnit parseCompilationUnit(ICompilationUnit icu, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, 1);
		CompilationUnit cu;
		if(compilationUnitHasCompilationErrors(icu)) {
			cu = parseCompilationUnitWithErrors(icu, sub.newChild(1));
		} else {
			cu = parseCompilationUnitWithoutErrors(icu, sub.newChild(1));
		}
		return cu;
	}
	
	//overridden in PPAJavaProjectModel
	protected CompilationUnit parseCompilationUnitWithErrors(ICompilationUnit icu, IProgressMonitor monitor) {
		if(ignoreCompilationErrors) {
			EkekoPlugin.getConsoleStream().println("Parsing file with compilation errors (not recommended): " + icu.getElementName());	
			return parse(icu, monitor);
		} else {
			EkekoPlugin.getConsoleStream().println("Not parsing file with compilation errors (configurable in project property page): " + icu.getElementName());	
			monitor.worked(1);
			return null;
		}
	}
	
	protected CompilationUnit parseCompilationUnitWithoutErrors(ICompilationUnit icu, IProgressMonitor monitor) {
		return parse(icu, monitor);
	}
	
	
	protected void gatherInformationFromCompilationUnits() {
		final long startTime = System.currentTimeMillis();
		for(CompilationUnit cu : getCompilationUnits()) 	
			addInformationFromVisitor(visitCompilationUnitForInformation(cu));
		final long duration = System.currentTimeMillis() - startTime;
		EkekoPlugin.getConsoleStream().println("Gathered information from JDT compilation units in " + duration + "ms");	
	}

	
	public void typeHierarchyChanged(ITypeHierarchy typeHierarchy) {
		if(!typeHierarchy.exists())
			removeTypeHierarchy(typeHierarchy);
		try {
			typeHierarchy.refresh(null);
		} catch (JavaModelException e) {
			removeTypeHierarchy(typeHierarchy);
			e.printStackTrace();
		}			
	}
	
	private void removeTypeHierarchy(ITypeHierarchy typeHierarchy) {
		typeHierarchy.removeTypeHierarchyChangedListener(this);
		itype2typehierarchy.remove(typeHierarchy.getType());
	}
	
	private void addTypeHierarchy(ITypeHierarchy typeHierarchy) {
		typeHierarchy.addTypeHierarchyChangedListener(this);
		itype2typehierarchy.put(typeHierarchy.getType(), typeHierarchy);	
	}

	class EkekoJavaProjectDeltaVisitor implements IResourceDeltaVisitor {
		//return true to continue visiting children.
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			//check whether this resource corresponds to an element in the Java model
			IJavaElement element = JavaCore.create(resource, javaProject); 
			if(element == null) 
				return false; //assuming children cannot exist in model either
			//if(resource.getType() != IResource.FILE) 
			//	return true; 
			if(IJavaElement.COMPILATION_UNIT != element.getElementType()) 
				return true; //assuming project itself or a package fragment root
			else  {
				//only interested in compilation units
				ICompilationUnit icu = (ICompilationUnit) element;
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					EkekoPlugin.getConsoleStream().println("Processing Java Project Delta: Added ICompilationUnit");
					processNewCompilationUnit(icu);
					break;
				case IResourceDelta.REMOVED:
					EkekoPlugin.getConsoleStream().println("Processing Java Project Delta: Removed ICompilationUnit");
					processRemovedCompilationUnit(icu);
					break;
				case IResourceDelta.CHANGED:
					EkekoPlugin.getConsoleStream().println("Processing Java Project Delta: Changed ICompilationUnit");
					processChangedCompilationUnit(icu);
					break;
				}
				return false;
			}
		}
	}

	
	public void processDelta(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		super.processDelta(delta,monitor);	
		delta.accept(new EkekoJavaProjectDeltaVisitor());	
	}
		
	private void processNewCompilationUnit(ICompilationUnit icu) {
		CompilationUnit cu = parseCompilationUnit(icu, null);
		if(cu == null)
			return;
		icu2ast.put(icu, cu);
		addInformationFromVisitor(visitCompilationUnitForInformation(cu));
		
	}
	
	private void processRemovedCompilationUnit(ICompilationUnit icu) {
		CompilationUnit old = icu2ast.remove(icu);
		if(old != null)
			removeInformationFromVisitor(visitCompilationUnitForInformation(old));
		
	}
	
	private void processChangedCompilationUnit(ICompilationUnit icu) {
		CompilationUnit old = icu2ast.remove(icu);
		if(old != null)
			removeInformationFromVisitor(visitCompilationUnitForInformation(old));
		CompilationUnit cu = parseCompilationUnit(icu, null);
		if(cu == null)
			return;
		icu2ast.put(icu, cu);
		addInformationFromVisitor(visitCompilationUnitForInformation(cu));
	}
	
	

	private IControlFlowGraph<ASTNode> newControlFlowGraph(MethodDeclaration m) {
		return new EclipseCFG(m);
	}
	
	
	public static String sootSignatureForFieldVariableBinding(IVariableBinding binding) {
		ITypeBinding declaringClass = binding.getDeclaringClass();
		if(declaringClass == null)
			return null;
		String binaryName = declaringClass.getBinaryName();
		if(binaryName == null)
			return null;
		return "<" + binaryName + ": " + sootSubsignatureTypeStringForEclipseTypeBinding(binding.getType()) + " " + binding.getName() + ">";	
	}

	public static String sootSignatureForMethodBinding(IMethodBinding b) {
		
		IMethodBinding binding = b.getMethodDeclaration(); //from parameterized to generic one
		
		ITypeBinding declaringClass = binding.getDeclaringClass();
		if(declaringClass == null)
			return null;
		String binaryName = declaringClass.getBinaryName();
		if(binaryName == null)
			return null;
		return "<" + binaryName + ": " + sootSubsignatureForMethodBinding(binding) + ">";	
	}

	public static String sootSubsignatureForMethodBinding(IMethodBinding binding) {
		if(binding.isConstructor())
			return sootSubsignatureForConstructorBinding(binding);
		else 
			return sootSubsignatureForNonConstructorMethodBinding(binding);
	}
	
	
	
	/*
	 * 
	 * "Returns the Soot subsignature of the given binding for a coonstructor method declaration."
	"As can be seen at http://tns-www.lcs.mit.edu/manuals/java-1.1.1/guide/innerclasses/spec/innerclasses.doc2.html, inner class constructors (except static classes) get an extra argument referencing their enclosing instance.
     If a class also contains private final fields val$XXX where XXX is the name of a local in the enclosing scope, they are also initialized in the constructor which results in extra synthetic arguments."
	"All these synthetic fields are initialized by constructor parameters, which have the same names as the fields they initialize. If one of the parameters is the innermost enclosing instance, it is the first. All such constructor parameters are deemed to be synthetic. If the compiler determines that the synthetic field's value is used only in the code of the constructor, it may omit the field itself, and use only the parameter to implement variable references (http://saloon.javaranch.com/cgi-bin/ubb/ultimatebb.cgi?ubb=get_topic&f=33&t=019435). "
	"Note that the last sentence may cause havoc since we are relying on a class'' synthetic fields to determine the constructor's signature."
	 * 
	 */
	public static String sootSubsignatureForConstructorBinding(IMethodBinding binding) {
		ITypeBinding declaringClass = binding.getDeclaringClass();
		ITypeBinding enclosingClass = declaringClass.getDeclaringClass();
		ITypeBinding[]  parameterTypes = binding.getParameterTypes();
		//TODO:	Cava used soot synthetic fields for more precise information 
		if(enclosingClass!=null) 
			if(!Modifier.isStatic(declaringClass.getModifiers())) {
				ITypeBinding[]  newParameterTypes = new ITypeBinding[parameterTypes.length + 1];
				System.arraycopy(parameterTypes, 0, newParameterTypes, 1, parameterTypes.length);
				newParameterTypes[0] = enclosingClass;
				parameterTypes = newParameterTypes;
			}
		return "void " + "<init>(" + sootSubsignatureTypeStringForCollectionOfTypes(parameterTypes) + ")"; 
	}


	/*
	 *  Returns the Soot subsignature of the given binding for a non-constructor (i.e. regular) method declaration.

	Example:
		Method declaration: public <A>A instanceMethod(int a,Collection<String> b,Collection<int[]> c,Collection d,A e)
		Method binding: A instanceMethod(int, Collection<java.lang.String>, Collection<int[]>, Collection#RAW, A)
		Method subsignature: java.lang.Object instanceMethod(int,java.util.Collection,java.util.Collection,java.util.Collection,java.lang.Object)
	 */
	public static String sootSubsignatureForNonConstructorMethodBinding(IMethodBinding binding) {
		
		return sootSubsignatureTypeStringForEclipseTypeBinding(binding.getReturnType())
				+ " " 
				+ binding.getName() 
				+ "("
				+ sootSubsignatureTypeStringForCollectionOfTypes(binding.getParameterTypes())
				+ ")"
				;
	}
	
		
	
	public static String sootSubsignatureTypeStringForCollectionOfTypes(ITypeBinding[] types) {
		if (types.length == 0) 
			return "";
		String subSignature = sootSubsignatureTypeStringForEclipseTypeBinding(types[0]);
		for(int i=1; i <types.length;i++) {
			subSignature += ",";
			subSignature += sootSubsignatureTypeStringForEclipseTypeBinding(types[i]);
		}
		return subSignature;
	}

	/*
	 * Converts an eclipse type binding to a string suitable for looking up e.g. methods by their subsignature in Soot.
	 */	
	public static String sootSubsignatureTypeStringForEclipseTypeBinding(ITypeBinding t) {
		
		ITypeBinding type = t;
		//get to the absolute base of the array
		//while(type.isArray())
		//	type = type.getComponentType();
		if(type.isArray())
			type = type.getElementType();
	
		//get rid of anything generic
		type = type.getErasure();
		String s;
		if(type.isPrimitive())
			s = type.getQualifiedName(); //soot expects java.lang.int instead of I
		else
			s = type.getBinaryName(); //correctly handles inner classes and anonoymous classes
		for(int i=0;i<t.getDimensions();i++)
			s = s + "[]";
		return s;
	}

	protected TableGatheringVisitor visitCompilationUnitForInformation(CompilationUnit cu) {
		TableGatheringVisitor v = new TableGatheringVisitor();
		cu.accept(v);
		return v;
	}
	
	public static String keyForMethodDeclaration(MethodDeclaration m) {
		if(m == null)
			return null;
		IMethodBinding binding = m.resolveBinding();
		if(binding == null)
			return null;
		return sootSignatureForMethodBinding(binding);
	}
	
	
	private static String keyForAbstractTypeDeclaration(AbstractTypeDeclaration t) {
		ITypeBinding binding = t.resolveBinding();
		if(binding == null)
			return null;
		return binding.getBinaryName();
	}
	
	public static String keyForTypeDeclaration(TypeDeclaration t) {
		return keyForAbstractTypeDeclaration(t);
	}
	
	public static String keyForEnumDeclaration(EnumDeclaration e) {
		return keyForAbstractTypeDeclaration(e);	
	}
	
	public static String keyForAnnotationTypeDeclaration(AnnotationTypeDeclaration a) {
		return keyForAbstractTypeDeclaration(a);	
	}

	public static String keyForAnonymousClassDeclaration(AnonymousClassDeclaration t) {
		ITypeBinding binding = t.resolveBinding();
		if(binding == null)
			return null;
		return binding.getBinaryName();
	}
	
		
	protected void addInformationFromVisitor(TableGatheringVisitor v) {		
		nodes.addAll(v.visitedNodes);
		
		expressions.addAll(v.expressions);
		
		statements.addAll(v.statements);
		
		for(TypeDeclaration t : v.typeDeclarations) {
			String key = keyForTypeDeclaration(t);
			if(key != null)
				typeDeclarations.put(key,t);
			else 
				typeDeclarationsWithoutBinding.add(t);
		}
		
		for(MethodDeclaration m : v.methodDeclarations) {
			String key = keyForMethodDeclaration(m);
			if(key != null) {
				methodDeclarations.put(key,m);
				//addOverriddenMethods(m);
			}
			else 
				methodDeclarationsWithoutBinding.add(m);			
		}
		
		fieldDeclarations.addAll(v.fieldDeclarations);
		
		singleVariableDeclarations.addAll(v.singleVariableDeclarations);
		
		enumConstantDeclarations.addAll(v.enumConstantDeclarations);
		
		annotationTypeMemberDeclarations.addAll(v.annotationTypeMemberDeclarations);
		
		types.addAll(v.types);
		
		fieldAccessLikeNodes.addAll(v.fieldAccessLikeNodes);
		
		invocationLikeNodes.addAll(v.invocationLikeNodes);
		

		
		for(EnumDeclaration e : v.enumDeclarations) {
			String key = keyForEnumDeclaration(e);
			if(key != null)
				enumDeclarations.put(key,e);
			else 
				enumDeclarationsWithoutBinding.add(e);
		}
		
		for(AnnotationTypeDeclaration a : v.annotationTypeDeclarations) {
			String key = keyForAnnotationTypeDeclaration(a);
			if(key != null)
				annotationTypeDeclarations.put(key,a);
			else 
				annotationTypeDeclarationsWithoutBinding.add(a);
		}
		
		for(AnonymousClassDeclaration c : v.anonymousClassDeclarations) {
			String key = keyForAnonymousClassDeclaration(c);
			if(key != null)
				anonymousClassDeclarations.put(key,c);
			else 
				anonymousClassDeclarationsWithoutBinding.add(c);
		}
		
		
		
	}	
		
	/*
	private void addOverriddenMethods(MethodDeclaration m) {
		Set<MethodDeclaration> overriddenMethods = new HashSet<>(computeOverriddenMethods(m));
		for(MethodDeclaration overridden : overriddenMethods) {
			Set<MethodDeclaration> oldOverriders = methodOverriders.get(overridden);
			if(oldOverriders == null) {
				oldOverriders = new HashSet<>();
				methodOverriders.put(overridden, oldOverriders);
			}
			oldOverriders.add(m);
		}
		methodOverridden.put(m, overriddenMethods);
	}
	
	private void removeOverriddenMethods(MethodDeclaration m) {
		Set<MethodDeclaration> overriddenMethods = methodOverridden.remove(m);
		for(MethodDeclaration overridden : overriddenMethods) {
			Set<MethodDeclaration> oldOverriders = methodOverriders.get(overridden);
			if(oldOverriders != null) {
				oldOverriders.remove(m);
			}
		}
	}

	*/
	
	
	protected void addControlFlowGraphInformationForMethodDeclaration(MethodDeclaration m) {
		//empty because building the new graphs lazily
	}

	protected void removeInformationFromVisitor(TableGatheringVisitor v) {
		
		nodes.removeAll(v.visitedNodes);
		
		expressions.removeAll(v.expressions);

		statements.removeAll(v.statements);
		
		for(TypeDeclaration t : v.typeDeclarations) {
			String key = keyForTypeDeclaration(t);
			if(key != null)
				typeDeclarations.remove(key,t);
			else 
				typeDeclarationsWithoutBinding.remove(t);
		}
		
		for(MethodDeclaration m : v.methodDeclarations) {
			String key = keyForMethodDeclaration(m);
			if(key != null)
				methodDeclarations.remove(key,m);
			else 
				methodDeclarationsWithoutBinding.remove(m);
			controlFlowGraphs.remove(m);			
			//removeOverriddenMethods(m);
		}
		
		
		fieldDeclarations.removeAll(v.fieldDeclarations);
		
		types.removeAll(v.types);
		
		fieldAccessLikeNodes.removeAll(v.fieldAccessLikeNodes);

		invocationLikeNodes.removeAll(v.invocationLikeNodes);
		
		annotationTypeMemberDeclarations.removeAll(v.annotationTypeMemberDeclarations);

		singleVariableDeclarations.removeAll(v.singleVariableDeclarations);

		enumConstantDeclarations.removeAll(v.enumConstantDeclarations);
		
		for(EnumDeclaration e : v.enumDeclarations) {
			String key = keyForEnumDeclaration(e);
			if(key != null)
				enumDeclarations.remove(key,e);
			else 
				enumDeclarationsWithoutBinding.remove(e);
		}
		
		for(AnnotationTypeDeclaration a : v.annotationTypeDeclarations) {
			String key = keyForAnnotationTypeDeclaration(a);
			if(key != null)
				annotationTypeDeclarations.remove(key,a);
			else 
				annotationTypeDeclarationsWithoutBinding.remove(a);
		}
		
		for(AnonymousClassDeclaration c : v.anonymousClassDeclarations) {
			String key = keyForAnonymousClassDeclaration(c);
			if(key != null)
				anonymousClassDeclarations.remove(key,c);
			else 
				anonymousClassDeclarationsWithoutBinding.remove(c);
		}
		
					
	}
	
	
	
	public ASTNode getDeclaringASTNode(ICompilationUnit icu, String key) {
		CompilationUnit cu = getCompilationUnit(icu);
		return cu.findDeclaringNode(key);
	}
	
	public MethodDeclaration getDeclaringASTNode(IMethod imethod) {
		ICompilationUnit icu = imethod.getCompilationUnit();
		if(icu == null) //from source
			return null;
		return (MethodDeclaration) getDeclaringASTNode(icu, imethod.getKey());
	}
	
	public FieldDeclaration getDeclaringASTNode(IField ifield) {
		ICompilationUnit icu = ifield.getCompilationUnit();
		if(icu == null) //from source
			return null;
		return (FieldDeclaration) getDeclaringASTNode(icu, ifield.getKey());
	}
	
	public TypeDeclaration getDeclaringASTNode(IType itype) {
		ICompilationUnit icu = itype.getCompilationUnit();
		if(icu == null) //from source
			return null;
		return (TypeDeclaration) getDeclaringASTNode(icu, itype.getKey());
	}
	
	
	public MethodDeclaration getDeclaringASTNode(IMethodBinding ibinding) {
		IMethod iJavaElement = (IMethod) ibinding.getJavaElement();
		if(iJavaElement == null)
			return null;
		ICompilationUnit icu = iJavaElement.getCompilationUnit();
		if(icu == null)
			return null;
		return (MethodDeclaration) getDeclaringASTNode(icu, ibinding.getKey());
	}
	
	//only for fields
	public FieldDeclaration getDeclaringASTNode(IVariableBinding ibinding) {
		if(!ibinding.isField())
			return null;
		IField iJavaElement = (IField) ibinding.getJavaElement();
		if(iJavaElement == null)
			return null;
		ICompilationUnit icu = iJavaElement.getCompilationUnit();
		if(icu == null)
			return null;
		return (FieldDeclaration) getDeclaringASTNode(icu, ibinding.getKey());
	}
	

	public TypeDeclaration getDeclaringASTNode(ITypeBinding ibinding) {
		IType iJavaElement = (IType) ibinding.getJavaElement();
		if(iJavaElement == null)
			return null;
		ICompilationUnit icu = iJavaElement.getCompilationUnit();
		if(icu == null)
			return null;
		return (TypeDeclaration) getDeclaringASTNode(icu, ibinding.getKey());
	}

	
	public IBinding[] resolveBinding(IJavaElement[] elements) {
		ASTParser parser = ASTParser.newParser(JLS);
		parser.setProject(getJavaProject());
		return parser.createBindings(elements, new NullProgressMonitor());
	}
	
	public IBinding resolveBinding(IJavaElement element) {
		IJavaElement[] elements = {element};
		return resolveBinding(elements)[0];
	}
	
	/*
    public List<IMethodBinding> computeOverridingMethods(IMethodBinding methodBinding) throws JavaModelException  {
    	if(methodBinding.isConstructor() || Modifier.isStatic(methodBinding.getModifiers())) {
    		return Collections.EMPTY_LIST;
    	}
        ITypeBinding declTypeBinding = methodBinding.getDeclaringClass();
        IType itype = (IType) declTypeBinding.getJavaElement();
        IMethod imethod = (IMethod) methodBinding.getJavaElement();
        ITypeHierarchy hierarchy = itype.newTypeHierarchy(new NullProgressMonitor());
        IType[] subtypes = hierarchy.getAllSubtypes(itype);
		ArrayList<IMethod> candidates = new ArrayList<IMethod>();
        for (IType subtype : subtypes) {
        	for (IMethod submethod : subtype.getMethods()) {
        		if (submethod.isSimilar(imethod)) {
        			candidates.add(submethod);
        		}
        	}
        }
		ArrayList<IMethodBinding> methods = new ArrayList<IMethodBinding>(candidates.size());
        for(IBinding binding : resolveBinding(candidates.toArray(new IMethod[candidates.size()]))) {
        	IMethodBinding submethodbinding = (IMethodBinding) binding;
        	if(submethodbinding.overrides(methodBinding)) {
        		methods.add(submethodbinding);
            }
        }
        return methods;
    }
 	
    
    public List<MethodDeclaration> computeOverridingMethods(MethodDeclaration methodDeclaration) throws JavaModelException  {
    	List<IMethodBinding> overridingMethods = computeOverridingMethods(methodDeclaration.resolveBinding());
    	return Lists.transform(overridingMethods, new Function<IMethodBinding, MethodDeclaration>() {
			@Override
			public MethodDeclaration apply(IMethodBinding binding) {
				return getDeclaringASTNode(binding);
			}
		});
    }
    
    public List<MethodDeclaration> computeOverriddenMethods(MethodDeclaration method) {
    	IMethodBinding binding = method.resolveBinding();
    	if(method.isConstructor() || Modifier.isStatic(method.getModifiers())) {
    		return Collections.EMPTY_LIST;
    	}
        ArrayList<MethodDeclaration> result= new ArrayList<MethodDeclaration>();
        ITypeBinding declaringClass= binding.getDeclaringClass();
        ITypeBinding[] superTypes= Bindings.getAllSuperTypes(declaringClass); 
        for (ITypeBinding type : superTypes) {
            for (IMethodBinding inheritedMethod : type.getDeclaredMethods()) {
                if (binding.overrides(inheritedMethod)) {
                	MethodDeclaration overridden = getDeclaringASTNode(binding);
                	if(overridden != null) {
                		result.add(overridden);
                	}
                }
            }
        }
        return result;
    }
    	
    */
	
	public void applyRewrite(ASTRewrite rewrite) throws MalformedTreeException, BadLocationException, JavaModelException{
		for(ICompilationUnit icu : getICompilationUnits()) {
			CompilationUnit cu = getCompilationUnit(icu);
			if(rewrite.getAST().equals(cu.getAST()))
				applyRewriteToICU(rewrite, icu);
		}	
	}
	
	public static void applyRewriteToICU(ASTRewrite rewrite, ICompilationUnit icu) throws MalformedTreeException, BadLocationException, JavaModelException{
		ICompilationUnit workingCopy = icu.getWorkingCopy(null);
		TextEdit edit = rewrite.rewriteAST();
		workingCopy.applyTextEdit(edit, null);
		workingCopy.commitWorkingCopy(false, null);
		workingCopy.discardWorkingCopy();	
		
	}
	
	public static void applyRewriteToNode(ASTRewrite rewrite, ASTNode node) throws MalformedTreeException, BadLocationException, JavaModelException{
		CompilationUnit cu = (CompilationUnit) node.getRoot();
		IJavaElement icu = cu.getJavaElement();
		applyRewriteToICU(rewrite,(ICompilationUnit)icu);
		
	}

	
	public Iterable<ASTNode> getNodes() {
		return nodes;
	}

	public <T extends ASTNode> Iterable<T> getNodesOfType(Class<T> classType) {
		HashSet<T> nodesOfType = new HashSet<T>(this.nodes.size());
		for(ASTNode node :  this.nodes) {
			if(classType.isInstance(node)) {
				nodesOfType.add((T) node);				
			}
		}
		return nodesOfType;
	}
	
	
	
	
}

