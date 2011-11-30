package flex.samples.dao;

import java.util.List;

/**
 * @author Christophe Coenraets
 */
public interface IGenericDAO< T > {

   public List< T > findAll();

   public List< T > findByName( String name );

   public T findById( int id );

   public T insert( T item );

   public boolean update( T item );

   public boolean delete( T item );

}