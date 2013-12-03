package damp.ekeko;

import java.lang.reflect.Modifier;
import java.util.Collection;
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
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

import dk.itu.smartemf.ofbiz.analysis.ControlFlowGraph;

public class JavaProjectModel extends ProjectModel implements ITypeHierarchyChangedListener {
	
	protected IJavaProject javaProject;
	private ASTParser parser; 
	
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
	
	private ConcurrentHashMap<MethodDeclaration,ControlFlowGraph> controlFlowGraphs;
	
	private Set<Statement> statements;	
	private Set<Expression> expressions;
	
	private ConcurrentHashMap<IType,ITypeHierarchy> itype2typehierarchy;
	
	public JavaProjectModel(IProject p) {
		super(p);
		javaProject = JavaCore.create(p);
		parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(javaProject);
		parser.setResolveBindings(true);
		clean();
	}

	public IJavaProject getJavaProject() {
		return javaProject;
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
	
	public ControlFlowGraph getControlFlowGraph(MethodDeclaration m) {
		return controlFlowGraphs.get(m);
	}
	
	public ITypeHierarchy getTypeHierarchy(IType type) throws JavaModelException {
		ITypeHierarchy typeHierarchy;
		if (!itype2typehierarchy.containsKey(type)) {
			typeHierarchy = type.newTypeHierarchy(null);
			addTypeHierarchy(typeHierarchy);
		}
		else
			typeHierarchy = itype2typehierarchy.get(type);
		return typeHierarchy;
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
	
	public CompilationUnit parse(ICompilationUnit icu, IProgressMonitor monitor) {
		//Removing the next three calls to parser results in null-bindings (even though already set in constructor)
		parser = ASTParser.newParser(AST.JLS4); //seems better than reusing the existing one (ran out of memory on azureus otherwise)			
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(javaProject);
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
		
		itype2typehierarchy = new ConcurrentHashMap<IType, ITypeHierarchy>();
	
		controlFlowGraphs = new  ConcurrentHashMap<MethodDeclaration,ControlFlowGraph>();
		
		types = java.util.Collections.newSetFromMap(new ConcurrentHashMap<Type,Boolean>());

		fieldAccessLikeNodes = java.util.Collections.newSetFromMap(new ConcurrentHashMap<ASTNode,Boolean>());
		invocationLikeNodes = java.util.Collections.newSetFromMap(new ConcurrentHashMap<ASTNode,Boolean>());

	
	}
	
	
	public void populate(IProgressMonitor monitor) throws CoreException {
		super.populate(monitor);
		String msg = "Populating JavaProjectModel for: " + javaProject.getElementName();
		System.out.println(msg);
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
			CompilationUnit cu;
			if(compilationUnitHasCompilationErrors(icu)) {
				cu = parseCompilationUnitWithErrors(icu, sub.newChild(1));
			} else {
				cu = parseCompilationUnitWithoutErrors(icu, sub.newChild(1));
			}
			if(cu != null)
				icu2ast.put(icu, cu);
		}		
	}
	
	//overridden in PPAJavaProjectModel
	protected CompilationUnit parseCompilationUnitWithErrors(ICompilationUnit icu, IProgressMonitor monitor) {
		System.out.println("Not parsing compilation unit because of compilation errors: " + icu.getElementName());	
		monitor.worked(1);
		return null;
	}
	
	protected CompilationUnit parseCompilationUnitWithoutErrors(ICompilationUnit icu, IProgressMonitor monitor) {
		return parse(icu, monitor);
	}
	
	
	protected void gatherInformationFromCompilationUnits() {
		final long startTime = System.currentTimeMillis();
		for(CompilationUnit cu : icu2ast.values()) 	
			addInformationFromVisitor(visitCompilationUnitForInformation(cu));
		final long duration = System.currentTimeMillis() - startTime;
		System.out.println("Gathered information from JDT compilation units in " + duration + "ms");	
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
					System.out.println("Added ICompilationUnit");
					processNewCompilationUnit(icu);
					break;
				case IResourceDelta.REMOVED:
					System.out.println("Removed ICompilationUnit");
					processRemovedCompilationUnit(icu);
					break;
				case IResourceDelta.CHANGED:
					System.out.println("Changed ICompilationUnit");
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
		CompilationUnit cu = parse(icu,null);
		icu2ast.put(icu, cu);
		addInformationFromVisitor(visitCompilationUnitForInformation(cu));
		
	}
	
	private void processRemovedCompilationUnit(ICompilationUnit icu) {
		CompilationUnit old = icu2ast.remove(icu);
		removeInformationFromVisitor(visitCompilationUnitForInformation(old));
		
	}
	
	private void processChangedCompilationUnit(ICompilationUnit icu) {
		CompilationUnit old = icu2ast.remove(icu);
		removeInformationFromVisitor(visitCompilationUnitForInformation(old));
		CompilationUnit cu = parse(icu,null);
		icu2ast.put(icu, cu);
		addInformationFromVisitor(visitCompilationUnitForInformation(cu));
	}
	
	

	private ControlFlowGraph newControlFlowGraph(MethodDeclaration m) {
		if(m.getBody() == null)
			return null;
		return new ControlFlowGraph(m);			
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
		while(type.isArray())
			type = type.getComponentType();
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
		for(Expression e : v.expressions)
			expressions.add(e);
		
		for(Statement s : v.statements)
			statements.add(s);
		
		for(TypeDeclaration t : v.typeDeclarations) {
		
			
			String key = keyForTypeDeclaration(t);
			if(key != null)
				typeDeclarations.put(key,t);
			else 
				typeDeclarationsWithoutBinding.add(t);
		}
		
		for(MethodDeclaration m : v.methodDeclarations) {
			String key = keyForMethodDeclaration(m);
			if(key != null)
				methodDeclarations.put(key,m);
			else 
				methodDeclarationsWithoutBinding.add(m);			
			addControlFlowGraphInformationForMethodDeclaration(m);
		}
		
		for(FieldDeclaration f : v.fieldDeclarations) 
			fieldDeclarations.add(f);		
		
		for(SingleVariableDeclaration svd : v.singleVariableDeclarations) 
			singleVariableDeclarations.add(svd)	;
		
		for(EnumConstantDeclaration ecd : v.enumConstantDeclarations) 
			enumConstantDeclarations.add(ecd);
		

		for(AnnotationTypeMemberDeclaration atmd : v.annotationTypeMemberDeclarations) 
			annotationTypeMemberDeclarations.add(atmd);
		
		for(Type t : v.types) 
			types.add(t);
		
		for(ASTNode n : v.fieldAccessLikeNodes) 
			fieldAccessLikeNodes.add(n);

		for(ASTNode n : v.invocationLikeNodes) 
			invocationLikeNodes.add(n);


		
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
		

	protected void addControlFlowGraphInformationForMethodDeclaration(MethodDeclaration m) {
		ControlFlowGraph cfg = newControlFlowGraph(m);
		if(cfg != null)
			controlFlowGraphs.put(m,cfg);
		
	}

	protected void removeInformationFromVisitor(TableGatheringVisitor v) {
		for(Expression e : v.expressions)
			expressions.remove(e);
		
		for(Statement s : v.statements)
			statements.remove(s);

		
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
		}
		
		for(FieldDeclaration f : v.fieldDeclarations) 
			fieldDeclarations.remove(f);
		
		for(Type t : v.types) 
			types.remove(t);
		
		for(ASTNode n : v.fieldAccessLikeNodes) 
			fieldAccessLikeNodes.remove(n);

		for(ASTNode n : v.invocationLikeNodes) 
			invocationLikeNodes.remove(n);
		
		for(AnnotationTypeMemberDeclaration atmd : v.annotationTypeMemberDeclarations) 
			annotationTypeMemberDeclarations.remove(atmd);

		
		for(SingleVariableDeclaration svd : v.singleVariableDeclarations) 
			singleVariableDeclarations.remove(svd);

		for(EnumConstantDeclaration ecd : v.enumConstantDeclarations) 
			enumConstantDeclarations.remove(ecd);

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
	
	public static void applyRewriteToICU(ASTRewrite rewrite, ICompilationUnit icu) throws MalformedTreeException, BadLocationException, JavaModelException{
		ICompilationUnit workingCopy = icu.getWorkingCopy(null);
		TextEdit edit = rewrite.rewriteAST();
		workingCopy.applyTextEdit(edit, null);
		workingCopy.commitWorkingCopy(false, null);
		workingCopy.discardWorkingCopy();	
	}
	
	public static void applyRewriteToNode(ASTRewrite rewrite, ASTNode node) throws MalformedTreeException, BadLocationException, JavaModelException{
		CompilationUnit cu = (CompilationUnit)node.getRoot();
		IJavaElement icu = cu.getJavaElement();
		applyRewriteToICU(rewrite,(ICompilationUnit)icu);
	}


	

	
}

