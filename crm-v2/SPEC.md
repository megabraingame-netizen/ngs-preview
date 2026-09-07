# NamiWin CRM v2

## Product principle
Action-first, project-aware CRM for NamiWin. Sales opportunity and execution project are separate entities. Every open opportunity must have a next action.

## Primary navigation
1. Command Center
2. Pipeline
3. Customers
4. Projects
5. Activities
6. Quotes
7. Reports
8. Settings

## Command Center
- Today / overdue follow-ups
- Hot opportunities
- Stalled opportunities
- Pipeline value and weighted value in IRR
- Won this month
- Active projects
- Quick Add

## Sales lifecycle
Lead -> Qualified -> Survey -> Quotation -> Negotiation -> Contract -> Won/Lost

## Project lifecycle
Pre-measure/list -> Quotation -> Contract meeting -> Substructure inspection -> Final measurement -> Production -> Glass measurement -> Shipment -> Floor distribution -> Installation -> Optional glass re-measure -> Glass shipment/distribution -> Glass installation -> Provisional handover -> Seal/gasket/waterproofing -> Adjustment -> Final handover

## Core entities
Account, Contact, Lead, Opportunity, Project, ProjectContactRole, Activity, Task, Call, Meeting, Note, Quote, QuoteVersion, File, StageHistory, User, Team, Tag, Source, Competitor, LostReason, Notification, AuditLog.

## Customer 360
- Account details
- Related contacts and roles
- Opportunities
- Projects
- Financial summary
- Activity timeline
- Next action
- Files and quotes

## Opportunity rules
- Amount in IRR
- Probability 0..100
- Weighted value = amount * probability
- Next action required for every open opportunity
- Stale warning after configurable inactivity period
- Lost reason required on Lost
- Won creates a Project without duplicate data entry

## Project rules
- Multiple contacts with roles: owner, investor, builder, executor, project manager, architect, facade designer, supervisor, procurement, contractor, supplier, referrer
- Separate execution timeline from sales pipeline
- Stage history is immutable/auditable
- Optional glass stages supported

## Mobile UX
Bottom nav: Today, Customers, Projects, Add, More.
Fast actions: call, WhatsApp/deep-link, map, photo, voice note, result of call, next follow-up.

## Desktop UX
RTL command-center layout, compact sidebar, global search, keyboard-friendly quick add, kanban pipeline, customer/project split panes.

## Data / sync architecture
Local-first client database + central API + PostgreSQL + sync queue. Offline writes are queued. Conflict resolution uses entity version + updated_at; sensitive destructive conflicts require user resolution.

## Security
Role-based permissions, audit log, soft-delete, encrypted transport, server-side authorization, backups, no credentials stored in plaintext.

## Phase 1 acceptance criteria
- Create/edit/search Account and Contact
- Multiple Contacts per Account
- Create Opportunity with stage/amount/probability/next action
- Kanban stage movement writes StageHistory
- Today and overdue activity queues
- Customer 360 timeline
- Won -> Project conversion
- Full NamiWin project stage timeline
- Dashboard KPIs
- Backup/export
- RTL Persian UI, IRR formatting, Persian-date presentation
- Android and Windows builds from one tested release tag
