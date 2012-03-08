/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.MetadataSourceProcessingOrder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.BagType;
import org.hibernate.type.SetType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class BasicCollectionBindingTests extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

//	@Test
//	public void testAnnotations() {
//		doTest( MetadataSourceProcessingOrder.ANNOTATIONS_FIRST );
//	}

	@Test
	public void testHbm() {
		doTest( MetadataSourceProcessingOrder.HBM_FIRST );
	}

	private void doTest(MetadataSourceProcessingOrder processingOrder) {
		MetadataSources sources = new MetadataSources( serviceRegistry );
//		sources.addAnnotatedClass( EntityWithBasicCollections.class );
		sources.addResource( "org/hibernate/metamodel/spi/binding/EntityWithBasicCollections.hbm.xml" );
		MetadataImpl metadata = (MetadataImpl) sources.getMetadataBuilder().with( processingOrder ).buildMetadata();

		final EntityBinding entityBinding = metadata.getEntityBinding( EntityWithBasicCollections.class.getName() );
		final EntityIdentifier entityIdentifier = entityBinding.getHierarchyDetails().getEntityIdentifier();
		assertNotNull( entityBinding );

		// TODO: this will fail until HHH-7121 is fixed
		//assertTrue( entityBinding.getPrimaryTable().locateColumn( "`name`" ).isUnique() );

		PluralAttributeBinding bagBinding = metadata.getCollection( EntityWithBasicCollections.class.getName() + ".theBag" );
		assertNotNull( bagBinding );
		assertSame( bagBinding, entityBinding.locateAttributeBinding( "theBag" ) );
		assertNotNull( bagBinding.getCollectionTable() );
		assertEquals( Identifier.toIdentifier( "`EntityWithBasicCollections_theBag`" ), bagBinding.getCollectionTable().getLogicalName() );
		PluralAttributeKeyBinding bagKeyBinding = bagBinding.getPluralAttributeKeyBinding();
		assertSame( bagBinding, bagKeyBinding.getPluralAttributeBinding() );
		HibernateTypeDescriptor bagHibernateTypeDescriptor = bagBinding.getHibernateTypeDescriptor();
		assertNull( bagHibernateTypeDescriptor.getExplicitTypeName() );
		assertEquals( Collection.class.getName(), bagHibernateTypeDescriptor.getJavaTypeName() );
		assertTrue( bagHibernateTypeDescriptor.getTypeParameters().isEmpty() );
		assertTrue( bagHibernateTypeDescriptor.getResolvedTypeMapping() instanceof BagType );
		assertFalse( bagHibernateTypeDescriptor.getResolvedTypeMapping().isComponentType() );
		assertEquals( EntityWithBasicCollections.class.getName() + ".theBag", ( (BagType) bagHibernateTypeDescriptor.getResolvedTypeMapping() ).getRole() );

		ForeignKey fkBag = bagKeyBinding.getForeignKey();
		assertNotNull( fkBag );
		assertSame( bagBinding.getCollectionTable(), fkBag.getSourceTable() );
		assertEquals( 1, fkBag.getColumnSpan() );
		Iterator<Column> fkBagColumnIterator = fkBag.getColumns().iterator();
		Iterator<Column> fkBagSourceColumnIterator = fkBag.getSourceColumns().iterator();
		assertNotNull( fkBagColumnIterator );
		assertNotNull( fkBagSourceColumnIterator );
		assertTrue( fkBagColumnIterator.hasNext() );
		assertTrue( fkBagSourceColumnIterator.hasNext() );
		assertEquals( Identifier.toIdentifier( "`owner_id`" ), fkBagColumnIterator.next().getColumnName() );
		assertEquals( Identifier.toIdentifier( "`owner_id`" ), fkBagSourceColumnIterator.next().getColumnName() );
		assertFalse( fkBagColumnIterator.hasNext() );
		assertFalse( fkBagSourceColumnIterator.hasNext() );
		assertSame( entityBinding.getPrimaryTable(), fkBag.getTargetTable() );
		assertSame( entityBinding.getPrimaryTable().getPrimaryKey().getColumns(), fkBag.getTargetColumns() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkBag.getDeleteRule() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkBag.getUpdateRule() );
		// FK name is null because no default FK name is generated until HHH-7092 is fixed
		assertNull( fkBag.getName() );
		checkEquals(
				entityIdentifier.getValueBinding().getHibernateTypeDescriptor(),
				bagKeyBinding.getHibernateTypeDescriptor()
		);
		assertEquals( 0, bagBinding.getCollectionTable().getPrimaryKey().getColumnSpan() );
		assertEquals(
				entityBinding.getPrimaryTable().getPrimaryKey().getColumns().iterator().next().getJdbcDataType(),
				bagKeyBinding.getForeignKey().getColumns().iterator().next().getJdbcDataType()
		);
		assertFalse( bagKeyBinding.isInverse() );
		assertEquals( PluralAttributeElementNature.BASIC, bagBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() );
		assertEquals( String.class.getName(), bagBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeName() );

		PluralAttributeBinding setBinding = metadata.getCollection( EntityWithBasicCollections.class.getName() + ".theSet" );
		assertNotNull( setBinding );
		assertSame( setBinding, entityBinding.locateAttributeBinding( "theSet" ) );
		assertNotNull( setBinding.getCollectionTable() );
		assertEquals( Identifier.toIdentifier( "`EntityWithBasicCollections_theSet`" ), setBinding.getCollectionTable().getLogicalName() );
		PluralAttributeKeyBinding setKeyBinding = setBinding.getPluralAttributeKeyBinding();
		assertSame( setBinding, setKeyBinding.getPluralAttributeBinding() );
		HibernateTypeDescriptor setHibernateTypeDescriptor = setBinding.getHibernateTypeDescriptor();
		assertNull( setHibernateTypeDescriptor.getExplicitTypeName() );
		assertEquals( Set.class.getName(), setHibernateTypeDescriptor.getJavaTypeName() );
		assertTrue( setHibernateTypeDescriptor.getTypeParameters().isEmpty() );
		assertTrue( setHibernateTypeDescriptor.getResolvedTypeMapping() instanceof SetType );
		assertFalse( setHibernateTypeDescriptor.getResolvedTypeMapping().isComponentType() );
		assertEquals( EntityWithBasicCollections.class.getName() + ".theSet", ( (SetType) setHibernateTypeDescriptor.getResolvedTypeMapping() ).getRole() );

		ForeignKey fkSet = setKeyBinding.getForeignKey();
		assertNotNull( fkSet );
		assertSame( setBinding.getCollectionTable(), fkSet.getSourceTable() );
		assertEquals( 1, fkSet.getColumnSpan() );
		Iterator<Column> fkSetColumnIterator = fkSet.getColumns().iterator();
		Iterator<Column> fkSetSourceColumnIterator = fkSet.getSourceColumns().iterator();
		assertNotNull( fkSetColumnIterator );
		assertNotNull( fkSetSourceColumnIterator );
		assertTrue( fkSetColumnIterator.hasNext() );
		assertTrue( fkSetSourceColumnIterator.hasNext() );
		assertEquals( Identifier.toIdentifier( "`pid`" ), fkSetColumnIterator.next().getColumnName() );
		assertEquals( Identifier.toIdentifier( "`pid`" ), fkSetSourceColumnIterator.next().getColumnName() );
		assertFalse( fkSetColumnIterator.hasNext() );
		assertFalse( fkSetSourceColumnIterator.hasNext() );
		assertSame( entityBinding.getPrimaryTable(), fkSet.getTargetTable() );
		assertSame( entityBinding.getPrimaryTable().getPrimaryKey().getColumns(), fkSet.getTargetColumns() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkSet.getDeleteRule() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkSet.getUpdateRule() );
		// FK is null because no default FK name is generated until HHH-7092 is fixed
		assertNull( fkSet.getName() );
		checkEquals(
				entityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding().getHibernateTypeDescriptor(),
				setKeyBinding.getHibernateTypeDescriptor()
		);
		assertFalse( setKeyBinding.isInverse() );
		assertEquals( 2, setBinding.getCollectionTable().getPrimaryKey().getColumnSpan() );
		Iterator<Column> setPrimaryKeyIterator = setBinding.getCollectionTable().getPrimaryKey().getColumns().iterator();
		assertEquals(
				entityBinding.getPrimaryTable().getPrimaryKey().getColumns().iterator().next().getJdbcDataType(),
				setPrimaryKeyIterator.next().getJdbcDataType()
		);
		assertEquals(
				setBinding.getCollectionTable().locateColumn( "`set_stuff`" ).getJdbcDataType(),
				setPrimaryKeyIterator.next().getJdbcDataType()
		);
		assertFalse( setPrimaryKeyIterator.hasNext() );
		assertSame(
				setBinding.getCollectionTable().getPrimaryKey().getColumns().iterator().next(),
				setKeyBinding.getForeignKey().getColumns().iterator().next()
		);
		assertEquals( PluralAttributeElementNature.BASIC, setBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() );
		assertEquals( String.class.getName(), setBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeName() );

		PluralAttributeBinding propertyRefSetBinding = metadata.getCollection( EntityWithBasicCollections.class.getName() + ".thePropertyRefSet" );
		assertNotNull( propertyRefSetBinding );
		assertSame( propertyRefSetBinding, entityBinding.locateAttributeBinding( "thePropertyRefSet" ) );
		assertNotNull( propertyRefSetBinding.getCollectionTable() );
		assertEquals( Identifier.toIdentifier( "`EntityWithBasicCollections_thePropertyRefSet`" ), propertyRefSetBinding.getCollectionTable().getLogicalName() );
		PluralAttributeKeyBinding propertyRefSetKeyBinding = propertyRefSetBinding.getPluralAttributeKeyBinding();
		assertSame( propertyRefSetBinding, propertyRefSetKeyBinding.getPluralAttributeBinding() );
		HibernateTypeDescriptor propertyRefSetHibernateTypeDescriptor = propertyRefSetBinding.getHibernateTypeDescriptor();
		assertNull( propertyRefSetHibernateTypeDescriptor.getExplicitTypeName() );
		assertEquals( Set.class.getName(), propertyRefSetHibernateTypeDescriptor.getJavaTypeName() );
		assertTrue( propertyRefSetHibernateTypeDescriptor.getTypeParameters().isEmpty() );
		assertTrue( propertyRefSetHibernateTypeDescriptor.getResolvedTypeMapping() instanceof SetType );
		assertFalse( propertyRefSetHibernateTypeDescriptor.getResolvedTypeMapping().isComponentType() );
		assertEquals(
				EntityWithBasicCollections.class.getName() + ".thePropertyRefSet",
				( (SetType) propertyRefSetHibernateTypeDescriptor.getResolvedTypeMapping() ).getRole()
		);

		ForeignKey fkPropertyRefSet = propertyRefSetKeyBinding.getForeignKey();
		assertNotNull( fkPropertyRefSet );
		assertSame( propertyRefSetBinding.getCollectionTable(), fkPropertyRefSet.getSourceTable() );
		assertEquals( 1, fkPropertyRefSet.getColumnSpan() );
		Iterator<Column> fkPropertyRefSetColumnIterator = fkPropertyRefSet.getColumns().iterator();
		Iterator<Column> fkPropertyRefSetSourceColumnIterator = fkPropertyRefSet.getSourceColumns().iterator();
		assertNotNull( fkPropertyRefSetColumnIterator );
		assertNotNull( fkPropertyRefSetSourceColumnIterator );
		assertTrue( fkPropertyRefSetColumnIterator.hasNext() );
		assertTrue( fkPropertyRefSetSourceColumnIterator.hasNext() );
		assertEquals( Identifier.toIdentifier( "`pid`" ), fkPropertyRefSetColumnIterator.next().getColumnName() );
		assertEquals( Identifier.toIdentifier( "`pid`" ), fkPropertyRefSetSourceColumnIterator.next().getColumnName() );
		assertFalse( fkPropertyRefSetColumnIterator.hasNext() );
		assertFalse( fkPropertyRefSetSourceColumnIterator.hasNext() );
		assertSame( entityBinding.getPrimaryTable(), fkPropertyRefSet.getTargetTable() );
		assertSame( entityBinding.getPrimaryTable().locateColumn( "`name`" ), fkPropertyRefSet.getTargetColumns().iterator().next() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkPropertyRefSet.getDeleteRule() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkPropertyRefSet.getUpdateRule() );
		// FK is null because no default FK name is generated until HHH-7092 is fixed
		assertNull( fkPropertyRefSet.getName() );
		checkEquals(
				entityBinding.locateAttributeBinding( "name" ).getHibernateTypeDescriptor(),
				propertyRefSetKeyBinding.getHibernateTypeDescriptor()
		);
		assertFalse( propertyRefSetKeyBinding.isInverse() );
		assertEquals( 2, propertyRefSetBinding.getCollectionTable().getPrimaryKey().getColumnSpan() );
		Iterator<Column> propertyRefSetPrimaryKeyIterator = propertyRefSetBinding.getCollectionTable().getPrimaryKey().getColumns().iterator();
		assertEquals(
				entityBinding.getPrimaryTable().locateColumn( "`name`" ).getJdbcDataType(),
				propertyRefSetPrimaryKeyIterator.next().getJdbcDataType()
		);
		assertEquals(
				propertyRefSetBinding.getCollectionTable().locateColumn( "`property_ref_set_stuff`" ).getJdbcDataType(),
				propertyRefSetPrimaryKeyIterator.next().getJdbcDataType()
		);
		assertFalse( propertyRefSetPrimaryKeyIterator.hasNext() );
		assertSame(
				propertyRefSetBinding.getCollectionTable().getPrimaryKey().getColumns().iterator().next(),
				propertyRefSetKeyBinding.getForeignKey().getColumns().iterator().next()
		);
		assertEquals( PluralAttributeElementNature.BASIC, propertyRefSetBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() );
		assertEquals( Integer.class.getName(), propertyRefSetBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeName() );
	}

	private void checkEquals(HibernateTypeDescriptor expected, HibernateTypeDescriptor actual) {
		assertEquals( expected.getExplicitTypeName(), actual.getExplicitTypeName() );
		assertEquals( expected.getJavaTypeName(), actual.getJavaTypeName() );
		assertEquals( expected.getTypeParameters(), actual.getTypeParameters() );
		assertEquals( expected.getResolvedTypeMapping(), actual.getResolvedTypeMapping() );
		assertEquals( expected.isToOne(), actual.isToOne() );
	}
}