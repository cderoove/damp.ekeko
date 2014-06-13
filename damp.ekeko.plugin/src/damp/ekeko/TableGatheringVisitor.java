package damp.ekeko;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.WildcardType;

public class TableGatheringVisitor extends ASTVisitor {
	
	public HashSet<TypeDeclaration> typeDeclarations = new HashSet<TypeDeclaration>();
	public HashSet<EnumDeclaration> enumDeclarations = new HashSet<EnumDeclaration>();
	public HashSet<AnonymousClassDeclaration> anonymousClassDeclarations = new HashSet<AnonymousClassDeclaration>();
	public HashSet<MethodDeclaration> methodDeclarations = new HashSet<MethodDeclaration>();
	public HashSet<FieldDeclaration> fieldDeclarations = new HashSet<FieldDeclaration>();
	public HashSet<AnnotationTypeDeclaration> annotationTypeDeclarations = new HashSet<AnnotationTypeDeclaration>();
	public HashSet<Statement> statements = new HashSet<Statement>();
	public HashSet<Expression> expressions = new HashSet<Expression>();
	public HashSet<SingleVariableDeclaration> singleVariableDeclarations = new HashSet<SingleVariableDeclaration>();
	public HashSet<EnumConstantDeclaration> enumConstantDeclarations = new HashSet<EnumConstantDeclaration>();
	public HashSet<AnnotationTypeMemberDeclaration> annotationTypeMemberDeclarations = new HashSet<AnnotationTypeMemberDeclaration>();
	public HashSet<Type> types = new HashSet<Type>();
	//were necessary to speed up the Exapus-like analyses
	public HashSet<ASTNode> fieldAccessLikeNodes = new HashSet<ASTNode>();
	public HashSet<ASTNode> invocationLikeNodes = new HashSet<ASTNode>();

	public HashSet<ASTNode> visitedNodes = new HashSet<ASTNode>();
	
	
	public boolean visit(AnnotationTypeMemberDeclaration atmd) {
		annotationTypeMemberDeclarations.add(atmd);
		return true;
	}
			
	public boolean visit(TypeDeclaration td) {
		typeDeclarations.add(td);
		return true;
	}

	public boolean visit(FieldDeclaration fd) {
		fieldDeclarations.add(fd);
		return true;
	}
	
	public boolean visit(EnumConstantDeclaration ecd) {
		enumConstantDeclarations.add(ecd);
		return true;
	}

	
	public boolean visit(SingleVariableDeclaration svd) {
		singleVariableDeclarations.add(svd);
		return true;
	}
	
	public boolean visit(AnnotationTypeDeclaration atd) {
		annotationTypeDeclarations.add(atd);
		return true;
	}
	
	public boolean visit(MethodDeclaration md) {
		methodDeclarations.add(md);
		return true;
	}
	
	public boolean visit(AnonymousClassDeclaration ac) {
		anonymousClassDeclarations.add(ac);
		return true;
	}
	
	public boolean visit(EnumDeclaration ec) {
		enumDeclarations.add(ec);
		return true;
	}

	public boolean visit(ImportDeclaration id) { 
		//See comment in preVisit
		return false;
	}
	
	public boolean visit(ArrayType t) {
		types.add(t);
		return false; //no need to descend, consider whole array type as a binary type
	}

	public boolean visit(ParameterizedType t) {
		types.add(t);
		return true; 
	}

	public boolean visit(PrimitiveType t) {
		types.add(t);
		return false;
	}

	public boolean visit(QualifiedType t) {
		types.add(t);
		return false; //no need to descend, rather consider whole type as binary
	}

	public boolean visit(SimpleType t) {
		types.add(t);
		return false;
	}

	public boolean visit(UnionType t) {
		types.add(t);
		return true;
	}

	public boolean visit(WildcardType t) {
		types.add(t);
		return true;
	}
	
	public boolean visit(FieldAccess f) {
		fieldAccessLikeNodes.add(f);
		return true;
	}
	
	public boolean visit(SuperFieldAccess f) {
		fieldAccessLikeNodes.add(f);
		return true;
	}
	
	public boolean visit(MethodInvocation i) {
		invocationLikeNodes.add(i);
		return true;
	}
	
	public boolean visit(SuperMethodInvocation i) {
		invocationLikeNodes.add(i);
		return true;
	}
	
	public boolean visit(ClassInstanceCreation i) {
		invocationLikeNodes.add(i);
		return true;
	}
	
	public boolean visit(ConstructorInvocation i) {
		invocationLikeNodes.add(i);
		return true;
	}
	
	public boolean visit(SuperConstructorInvocation i) {
		invocationLikeNodes.add(i);
		return true;
	}
	 
	public void preVisit(ASTNode n) {
		visitedNodes.add(n);

		if (n instanceof Expression) {
	
			if (n instanceof Name) {
				Name nam = (Name) n;
				//Try to weed out as many as possible without having to resolve the name
				//See remark above nam.resolveBinding
				StructuralPropertyDescriptor locationInParent = nam.getLocationInParent();
				if (locationInParent.equals(QualifiedName.QUALIFIER_PROPERTY))
					return;
				if (locationInParent.equals(MethodInvocation.NAME_PROPERTY))//costly to resolve
					return;
				if (locationInParent.equals(MethodDeclaration.NAME_PROPERTY))//costly to resolve
					return;
				//There are name expressions in JEdit for which the next line returns a NPE (seems to be within import declarations only)
				IBinding b = nam.resolveBinding();
				if(b != null)
					if(IBinding.VARIABLE != b.getKind())
						return;
					else {
						//has to be added to expressions
						IVariableBinding ivb = (IVariableBinding) b;
						if(ivb.isField()) //and to field access like nodes
							fieldAccessLikeNodes.add(nam);
					}
			}
			expressions.add((Expression) n);
			return;
		}
		if (n instanceof Statement) {
			if (n instanceof Block) 
				if(n.getParent() instanceof MethodDeclaration)
					return;
			statements.add((Statement) n);
			return;
		}
	}

}
