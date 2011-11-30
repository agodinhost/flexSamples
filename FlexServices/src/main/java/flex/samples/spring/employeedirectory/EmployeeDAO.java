package flex.samples.spring.employeedirectory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.flex.remoting.RemotingDestination;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

import flex.samples.employeedirectory.Employee;
import flex.samples.employeedirectory.IEmployeetDAO;

/**
 * Spring JDBC DAO Class.
 * Note that the EmployeeDirectory app calls it as a service too.
 * 
 * @author Christophe Coenraets.
 */
@Service( "employeeService" )
@RemotingDestination( channels = { "my-amf" } )
public class EmployeeDAO implements IEmployeetDAO {

   private final SimpleJdbcTemplate    template;
   private final SimpleJdbcInsert      insertContact;

   private final RowMapper< Employee > summaryRowMapper = getSummaryRowMapper();
   private final RowMapper< Employee > rowMapper        = getRowMapper();

   @Autowired
   public EmployeeDAO( DataSource dataSource ) {
      System.out.println( "EmployeeDAO constructor" );
      template = new SimpleJdbcTemplate( dataSource );
      insertContact = new SimpleJdbcInsert( dataSource ).withTableName( "EMPLOYEE" ).usingGeneratedKeyColumns( "ID" );
   }

   public List< Employee > findAll() {
      return template.query( SQL_FIND_ALL, summaryRowMapper );
   }

   public List< Employee > findByName( String name ) {
      return template.query( SQL_FIND_BY_NAME, summaryRowMapper, "%" + name.toUpperCase() + "%" );
   }

   public List< Employee > findByManager( int managerId ) {
      return template.query( SQL_FIND_BY_MANAGER, summaryRowMapper, managerId );
   }

   public Employee findById( int id ) {
      return template.queryForObject( SQL_FIND_BY_ID, rowMapper, id );
   }

   public Employee insert( Employee employee ) {
      Map< String, Object > parameters = new HashMap< String, Object >();
      parameters.put( "first_name", employee.getFirstName() );
      parameters.put( "last_name", employee.getLastName() );
      parameters.put( "title", employee.getTitle() );
      parameters.put( "department", employee.getDepartment() );
      if( employee.getManager() != null ) {
         parameters.put( "manager_id", employee.getManager().getId() );
      }
      parameters.put( "city", employee.getCity() );
      parameters.put( "office_phone", employee.getOfficePhone() );
      parameters.put( "cell_phone", employee.getCellPhone() );
      parameters.put( "email", employee.getEmail() );
      parameters.put( "picture", employee.getPicture() );
      Number id = insertContact.executeAndReturnKey( parameters );
      employee.setId( id.intValue() );
      return employee;
   }

   public boolean update( Employee employee ) {
      int count = template.update( SQL_UPDATE, //
            employee.getFirstName(), employee.getLastName(), employee.getTitle(), //
            employee.getDepartment(), employee.getManager().getId(), employee.getCity(), //
            employee.getOfficePhone(), employee.getCellPhone(), employee.getEmail(), //
            employee.getPicture(), employee.getId() );
      return count == 1;
   }

   public boolean delete( Employee employee ) {
      int count = template.update( SQL_DELETE, employee.getId() );
      return count == 1;
   }

   private static final ParameterizedRowMapper< Employee > getSummaryRowMapper() {

      return new ParameterizedRowMapper< Employee >() {

         public Employee mapRow( ResultSet rs, int rowNum ) throws SQLException {
            Employee employee = new Employee();
            employee.setId( rs.getInt( "id" ) );
            employee.setFirstName( rs.getString( "first_name" ) );
            employee.setLastName( rs.getString( "last_name" ) );
            employee.setTitle( rs.getString( "title" ) );
            return employee;
         }
      };
   }

   private static final ParameterizedRowMapper< Employee > getRowMapper() {

      return new ParameterizedRowMapper< Employee >() {

         public Employee mapRow( ResultSet rs, int rowNum ) throws SQLException {
            Employee employee = new Employee();
            employee.setId( rs.getInt( "id" ) );
            employee.setFirstName( rs.getString( "first_name" ) );
            employee.setLastName( rs.getString( "last_name" ) );
            employee.setTitle( rs.getString( "title" ) );
            employee.setDepartment( rs.getString( "department" ) );
            employee.setCity( rs.getString( "city" ) );
            employee.setOfficePhone( rs.getString( "office_phone" ) );
            employee.setCellPhone( rs.getString( "cell_phone" ) );
            employee.setEmail( rs.getString( "email" ) );
            employee.setPicture( rs.getString( "picture" ) );
            int managerId = rs.getInt( "manager_id" );
            if( managerId > 0 ) {
               Employee manager = new Employee();
               manager.setId( managerId );
               manager.setFirstName( rs.getString( "manager_first_name" ) );
               manager.setLastName( rs.getString( "manager_last_name" ) );
               employee.setManager( manager );
            }
            employee.setReportCount( rs.getInt( "report_count" ) );
            return employee;
         }
      };
   }

   private static final String SQL_FIND_ALL        = "SELECT  id, first_name, last_name, title " + //
                                                         "FROM employee " + //
                                                         "ORDER BY first_name, last_name";

   private static final String SQL_FIND_BY_NAME    = "SELECT id, first_name, last_name, title " + //
                                                         "FROM employee " + //
                                                         "WHERE UPPER(CONCAT(first_name, ' ', last_name)) " + //
                                                         "LIKE ? " + //
                                                         "ORDER BY first_name, last_name";

   private static final String SQL_FIND_BY_MANAGER = "SELECT id, first_name, last_name, title " + //
                                                         "FROM employee " + //
                                                         "WHERE manager_id=? " + //
                                                         "ORDER BY first_name, last_name";

   private static final String SQL_FIND_BY_ID      = "SELECT e.id, e.first_name, e.last_name, e.manager_id, e.title, " + //
                                                         "e.department, e.city, e.office_phone, e.cell_phone, " + //
                                                         "e.email, e.picture, m.first_name manager_first_name, " + //
                                                         "m.last_name manager_last_name, count(r.id) report_count " + //
                                                         "FROM employee as e " + //
                                                         "LEFT JOIN employee m on e.manager_id = m.id " + //
                                                         "LEFT JOIN employee r on e.id = r.manager_id " + //
                                                         "WHERE e.id = ? " + //
                                                         "GROUP BY e.id";

   private static final String SQL_UPDATE          = "UPDATE employee SET " + //
                                                         "first_name=?, last_name=?, title=?, deptartment=?, manager_id=?, " + //
                                                         "city=?, office_phone, cell_phone=?, email=?, picture=? " + //
                                                         "WHERE id=?";

   private static final String SQL_DELETE          = "DELETE FROM employee WHERE id=?";

}