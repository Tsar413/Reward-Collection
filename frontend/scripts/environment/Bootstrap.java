import com.study.rewardCollection.util.PasswordUtil;
import java.sql.*;
public class Bootstrap {
  public static void main(String[] args) throws Exception {
    String db=System.getenv("IT_DB_NAME");
    if (!db.matches("reward_collection_it_[A-Za-z0-9_]+")) throw new IllegalArgumentException("isolated DB required");
    try(Connection c=DriverManager.getConnection(System.getenv("IT_JDBC_URL"),System.getenv("SPRING_DATASOURCE_USERNAME"),System.getenv("SPRING_DATASOURCE_PASSWORD"))) {
      if(!db.equals(c.getCatalog())) throw new IllegalStateException("wrong database");
      c.setAutoCommit(false);
      try {
        try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM sys_user")){r.next();if(r.getInt(1)!=0)throw new IllegalStateException("refuse existing users");}
        long[] orgs=new long[2];
        for(int i=0;i<2;i++)try(var s=c.prepareStatement("INSERT INTO sys_org(org_code,org_name,status,remark,create_time,update_time) VALUES(?,?,1,'IT initialization',NOW(),NOW())",Statement.RETURN_GENERATED_KEYS)){
          s.setString(1,"IT_ORG_"+(i==0?"A":"B"));s.setString(2,"验收组织"+(i==0?"A":"B"));s.executeUpdate();try(var r=s.getGeneratedKeys()){r.next();orgs[i]=r.getLong(1);}
        }
        String salt=PasswordUtil.generateSalt();
        try(var s=c.prepareStatement("INSERT INTO sys_user(org_id,username,real_name,role,user_no,password_hash,password_salt,password_algorithm,status,remark,create_time,update_time) VALUES(?,'验收管理员','验收管理员','ADMIN','IT_ADM_A',?,?,?,1,'IT initialization',NOW(),NOW())")){
          s.setLong(1,orgs[0]);s.setString(2,PasswordUtil.hashPassword(System.getenv("IT_TEST_PASSWORD"),salt));s.setString(3,salt);s.setString(4,PasswordUtil.ALGORITHM);s.executeUpdate();
        }
        c.commit();System.out.println("Initialized two organizations and one administrator with project PasswordUtil; no credentials printed");
      } catch(Exception e){c.rollback();throw e;}
    }
  }
}
