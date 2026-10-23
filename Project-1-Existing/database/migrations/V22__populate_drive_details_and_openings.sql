-- ============================================================
-- V22__populate_drive_details_and_openings.sql
-- Populate comprehensive recruitment specifications for all placement drives
-- Eliminates null descriptions and ensures high-fidelity drive data
-- ============================================================

-- DRV001: Microsoft India - Software Development Engineer I
UPDATE PLACEMENT_DRIVE SET
    Job_Description = 'Architect and build scalable distributed cloud systems, high-throughput microservices, and AI-enabled platforms at Microsoft India. Looking for engineers passionate about algorithms, distributed data stores, and systems programming.',
    Location = 'Bengaluru / Hyderabad',
    Selection_Process = 'Round 1: Online Coding Assessment (DSA & Problem Solving) | Round 2: Data Structures & System Design | Round 3: Engineering Manager & Fitment',
    Openings = 15
WHERE Drive_Id = 'DRV001';

-- DRV002: Tata Consultancy Services - Associate Software Engineer
UPDATE PLACEMENT_DRIVE SET
    Job_Description = 'Develop and deploy enterprise software solutions, digital transformation workflows, and cloud-native applications for global Fortune 500 clients. Involves Java/Python backend development, database management, and agile CI/CD pipelines.',
    Location = 'Pan India (Chennai / Bengaluru / Pune)',
    Selection_Process = 'Round 1: TCS NQT Cognitive & Coding Assessment | Round 2: Technical Interview (Core CS, DBMS, OOP) | Round 3: HR & Managerial Interview',
    Openings = 50
WHERE Drive_Id = 'DRV002';

-- DRV003: Infosys Limited - Systems Engineer - Specialist
UPDATE PLACEMENT_DRIVE SET
    Job_Description = 'Design and implement full-stack enterprise applications, microservices, and automated data pipelines. Focus on modern cloud architectures, relational database optimization, and secure API integrations.',
    Location = 'Bengaluru / Mysuru / Hyderabad',
    Selection_Process = 'Round 1: InfyTQ Online Programming Test (Advanced Algorithms) | Round 2: Technical Interview (Database Internals & DSA) | Round 3: Leadership Assessment',
    Openings = 25
WHERE Drive_Id = 'DRV003';

-- DRV004: Amazon Development Centre - Cloud Support Associate
UPDATE PLACEMENT_DRIVE SET
    Job_Description = 'Provide architectural and operational support for AWS cloud services, serverless architectures, and distributed systems. Solve deep technical networking, Linux kernel, database, and virtualization challenges for global enterprise workloads.',
    Location = 'Bengaluru / Hyderabad',
    Selection_Process = 'Round 1: Amazon Online Assessment (Debugging & Cloud Systems) | Round 2: Technical Deep Dive (Networking & OS) | Round 3: Leadership Principles & Behavioral',
    Openings = 20
WHERE Drive_Id = 'DRV004';

-- DRV005: Oracle India Pvt Ltd - Associate Applications Engineer
UPDATE PLACEMENT_DRIVE SET
    Job_Description = 'Engineer cutting-edge cloud infrastructure, Autonomous Database tools, and enterprise SaaS solutions at Oracle. Work closely with relational database internals, SQL optimization, PL/SQL engine performance, and secure enterprise multi-tenancy.',
    Location = 'Bengaluru / Hyderabad',
    Selection_Process = 'Round 1: Oracle Technical Assessment (SQL, Algorithms & CS Fundamentals) | Round 2: Systems & DBMS Architecture Interview | Round 3: Director Discussion',
    Openings = 12
WHERE Drive_Id = 'DRV005';

-- DRV006: Goldman Sachs Services - Technology Analyst
UPDATE PLACEMENT_DRIVE SET
    Job_Description = 'Build low-latency trading engines, financial transaction systems, risk assessment platforms, and resilient quantitative analytics pipelines. Demands exceptional analytical rigor, algorithms mastery, and database concurrency handling.',
    Location = 'Bengaluru',
    Selection_Process = 'Round 1: HackerRank Technical Assessment (Advanced DSA & Math) | Round 2: Dual Technical Interview Rounds (Concurrency & Problem Solving) | Round 3: VP Fitment Round',
    Openings = 10
WHERE Drive_Id = 'DRV006';

-- DRV007: Flipkart Internet Pvt Ltd - Backend Software Engineer
UPDATE PLACEMENT_DRIVE SET
    Job_Description = 'Build high-scale e-commerce transactional platforms handling millions of queries per second during Big Billion Days. Design distributed caching, resilient payment state machines, and relational-to-NoSQL event streams.',
    Location = 'Bengaluru',
    Selection_Process = 'Round 1: Machine Coding Round (Object-Oriented Design & Clean Code) | Round 2: Problem Solving & Data Structures | Round 3: Hiring Manager & Culture Fit',
    Openings = 18
WHERE Drive_Id = 'DRV007';

-- DRV008: Cisco Systems India - Network Consulting Engineer
UPDATE PLACEMENT_DRIVE SET
    Job_Description = 'Design, test, and implement mission-critical enterprise network architectures, software-defined networking (SDN) controllers, and cloud security frameworks for global service providers and enterprises.',
    Location = 'Bengaluru / Chennai',
    Selection_Process = 'Round 1: Cisco Online Test (Algorithms, Computer Networks, OS) | Round 2: Technical Interview (Routing, Switching & Network Automation) | Round 3: HR Round',
    Openings = 15
WHERE Drive_Id = 'DRV008';

-- Ensure all other placement drives have reasonable defaults if dynamically added
UPDATE PLACEMENT_DRIVE SET
    Job_Description = 'Campus recruitment opening for engineering graduates involving software design, system integration, and collaborative product development.'
WHERE Job_Description IS NULL;

COMMIT;
