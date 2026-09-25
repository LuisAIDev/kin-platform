import psycopg2

conn = psycopg2.connect('postgresql://neondb_owner:npg_iRk2atlBzjq5@ep-spring-dawn-ayhwgk7a-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require')
cur = conn.cursor()

cur.execute("SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'rips_records' AND column_name = 'rips_type';")
print('Column:', cur.fetchall())

cur.execute("SELECT indexname FROM pg_indexes WHERE tablename = 'rips_records' AND indexname = 'idx_rips_records_rips_type';")
print('Index:', cur.fetchall())

cur.execute("SELECT version, description, success, installed_on FROM flyway_schema_history WHERE version = '72';")
print('Flyway:', cur.fetchall())

conn.close()