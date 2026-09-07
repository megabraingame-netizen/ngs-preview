using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace NamiWinCRM
{
    public class Customer { public string Name { get; set; } public string Phone { get; set; } public string Address { get; set; } }
    public class Project { public string Customer { get; set; } public string Title { get; set; } public string Stage { get; set; } public string Amount { get; set; } }
    public class FollowUp { public string Customer { get; set; } public string Note { get; set; } public string Date { get; set; } }
    public class AppData { public List<Customer> Customers { get; set; } = new List<Customer>(); public List<Project> Projects { get; set; } = new List<Project>(); public List<FollowUp> FollowUps { get; set; } = new List<FollowUp>(); }

    static class Program
    {
        [STAThread]
        static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }

    public class MainForm : Form
    {
        readonly string dataDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "NamiWinCRM");
        string dataFile;
        AppData data;
        TabControl tabs = new TabControl();
        DataGridView customersGrid = new DataGridView();
        DataGridView projectsGrid = new DataGridView();
        DataGridView followGrid = new DataGridView();
        Label stats = new Label();

        public MainForm()
        {
            Text = "NamiWin CRM";
            Width = 1200; Height = 760; StartPosition = FormStartPosition.CenterScreen;
            RightToLeft = RightToLeft.Yes; RightToLeftLayout = true;
            Font = new Font("Tahoma", 10f);
            dataFile = Path.Combine(dataDir, "data.json");
            LoadData(); BuildUi(); RefreshAll();
        }

        void BuildUi()
        {
            var header = new Panel { Dock = DockStyle.Top, Height = 82, Padding = new Padding(16) };
            var title = new Label { Text = "CRM نامی‌وین", AutoSize = true, Font = new Font("Tahoma", 18f, FontStyle.Bold), Dock = DockStyle.Right };
            stats.AutoSize = true; stats.Dock = DockStyle.Left; stats.TextAlign = ContentAlignment.MiddleLeft;
            header.Controls.Add(title); header.Controls.Add(stats);
            Controls.Add(header);

            tabs.Dock = DockStyle.Fill;
            tabs.TabPages.Add(MakeCustomersTab());
            tabs.TabPages.Add(MakeProjectsTab());
            tabs.TabPages.Add(MakeFollowUpsTab());
            Controls.Add(tabs);
        }

        TabPage MakeCustomersTab()
        {
            var page = new TabPage("مشتریان");
            customersGrid = MakeGrid();
            customersGrid.Columns.Add("Name", "نام مشتری");
            customersGrid.Columns.Add("Phone", "موبایل / تلفن");
            customersGrid.Columns.Add("Address", "آدرس");
            page.Controls.Add(customersGrid);
            var bar = MakeButtonBar();
            AddButton(bar, "افزودن مشتری", (s,e)=>AddCustomer());
            AddButton(bar, "حذف", (s,e)=>DeleteCustomer());
            page.Controls.Add(bar);
            return page;
        }

        TabPage MakeProjectsTab()
        {
            var page = new TabPage("پروژه‌ها");
            projectsGrid = MakeGrid();
            projectsGrid.Columns.Add("Customer", "کارفرما");
            projectsGrid.Columns.Add("Title", "نام پروژه");
            projectsGrid.Columns.Add("Stage", "مرحله");
            projectsGrid.Columns.Add("Amount", "مبلغ / ریال");
            page.Controls.Add(projectsGrid);
            var bar = MakeButtonBar();
            AddButton(bar, "افزودن پروژه", (s,e)=>AddProject());
            AddButton(bar, "تغییر مرحله", (s,e)=>ChangeStage());
            AddButton(bar, "حذف", (s,e)=>DeleteProject());
            page.Controls.Add(bar);
            return page;
        }

        TabPage MakeFollowUpsTab()
        {
            var page = new TabPage("پیگیری‌ها");
            followGrid = MakeGrid();
            followGrid.Columns.Add("Customer", "مشتری");
            followGrid.Columns.Add("Note", "یادداشت / نتیجه تماس");
            followGrid.Columns.Add("Date", "تاریخ پیگیری");
            page.Controls.Add(followGrid);
            var bar = MakeButtonBar();
            AddButton(bar, "ثبت پیگیری", (s,e)=>AddFollowUp());
            AddButton(bar, "حذف", (s,e)=>DeleteFollowUp());
            page.Controls.Add(bar);
            return page;
        }

        DataGridView MakeGrid()
        {
            return new DataGridView { Dock = DockStyle.Fill, ReadOnly = true, AllowUserToAddRows = false, AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill, SelectionMode = DataGridViewSelectionMode.FullRowSelect, MultiSelect = false, RowHeadersVisible = false };
        }

        FlowLayoutPanel MakeButtonBar() => new FlowLayoutPanel { Dock = DockStyle.Bottom, Height = 58, FlowDirection = FlowDirection.RightToLeft, Padding = new Padding(8) };
        void AddButton(Control parent, string text, EventHandler click) { var b = new Button { Text=text, AutoSize=true, Height=36, Padding=new Padding(12,0,12,0) }; b.Click += click; parent.Controls.Add(b); }

        string Ask(string title, string label, string preset="")
        {
            using (var f = new Form { Text=title, Width=440, Height=160, StartPosition=FormStartPosition.CenterParent, RightToLeft=RightToLeft.Yes, RightToLeftLayout=true })
            {
                var l = new Label { Text=label, Dock=DockStyle.Top, Height=30, Padding=new Padding(8) };
                var t = new TextBox { Text=preset, Dock=DockStyle.Top, Height=28 };
                var ok = new Button { Text="تأیید", DialogResult=DialogResult.OK, Dock=DockStyle.Bottom, Height=34 };
                f.Controls.Add(ok); f.Controls.Add(t); f.Controls.Add(l); f.AcceptButton=ok;
                return f.ShowDialog(this)==DialogResult.OK ? t.Text.Trim() : null;
            }
        }

        void AddCustomer()
        {
            var name=Ask("مشتری جدید","نام مشتری"); if(string.IsNullOrWhiteSpace(name)) return;
            var phone=Ask("مشتری جدید","شماره تماس"); var address=Ask("مشتری جدید","آدرس");
            data.Customers.Add(new Customer{Name=name,Phone=phone,Address=address}); SaveData(); RefreshAll();
        }

        void AddProject()
        {
            var customer=Ask("پروژه جدید","نام کارفرما / مشتری"); if(string.IsNullOrWhiteSpace(customer)) return;
            var title=Ask("پروژه جدید","نام پروژه"); if(string.IsNullOrWhiteSpace(title)) return;
            var amount=Ask("پروژه جدید","مبلغ به ریال");
            data.Projects.Add(new Project{Customer=customer,Title=title,Stage="پیش اندازه‌گیری",Amount=amount}); SaveData(); RefreshAll();
        }

        readonly string[] stages = { "پیش اندازه‌گیری", "پیش‌فاکتور", "جلسه قرارداد", "کارشناسی زیرسازی", "اندازه‌گیری نهایی", "تولید", "اندازه‌گیری شیشه", "ارسال بار", "حمل و توزیع طبقات", "نصب", "نصب شیشه", "تحویل موقت", "چسب / لاستیک / آب‌بندی", "ریگلاژ و تحویل نهایی" };
        void ChangeStage()
        {
            if(projectsGrid.SelectedRows.Count==0) return; int i=projectsGrid.SelectedRows[0].Index; if(i<0||i>=data.Projects.Count) return;
            using(var f=new Form{Text="مرحله پروژه",Width=460,Height=150,StartPosition=FormStartPosition.CenterParent,RightToLeft=RightToLeft.Yes,RightToLeftLayout=true})
            {
                var c=new ComboBox{Dock=DockStyle.Top,DropDownStyle=ComboBoxStyle.DropDownList}; c.Items.AddRange(stages); c.SelectedItem=data.Projects[i].Stage; if(c.SelectedIndex<0)c.SelectedIndex=0;
                var b=new Button{Text="ثبت مرحله",Dock=DockStyle.Bottom,DialogResult=DialogResult.OK,Height=36}; f.Controls.Add(b); f.Controls.Add(c);
                if(f.ShowDialog(this)==DialogResult.OK){data.Projects[i].Stage=c.SelectedItem.ToString();SaveData();RefreshAll();}
            }
        }

        void AddFollowUp()
        {
            var customer=Ask("پیگیری جدید","نام مشتری"); if(string.IsNullOrWhiteSpace(customer)) return;
            var note=Ask("پیگیری جدید","یادداشت / نتیجه تماس"); var date=Ask("پیگیری جدید","تاریخ پیگیری",DateTime.Now.ToString("yyyy/MM/dd"));
            data.FollowUps.Add(new FollowUp{Customer=customer,Note=note,Date=date}); SaveData(); RefreshAll();
        }

        void DeleteCustomer(){ if(customersGrid.SelectedRows.Count==0)return; int i=customersGrid.SelectedRows[0].Index; if(i>=0&&i<data.Customers.Count){data.Customers.RemoveAt(i);SaveData();RefreshAll();} }
        void DeleteProject(){ if(projectsGrid.SelectedRows.Count==0)return; int i=projectsGrid.SelectedRows[0].Index; if(i>=0&&i<data.Projects.Count){data.Projects.RemoveAt(i);SaveData();RefreshAll();} }
        void DeleteFollowUp(){ if(followGrid.SelectedRows.Count==0)return; int i=followGrid.SelectedRows[0].Index; if(i>=0&&i<data.FollowUps.Count){data.FollowUps.RemoveAt(i);SaveData();RefreshAll();} }

        void RefreshAll()
        {
            customersGrid.Rows.Clear(); foreach(var x in data.Customers) customersGrid.Rows.Add(x.Name,x.Phone,x.Address);
            projectsGrid.Rows.Clear(); foreach(var x in data.Projects) projectsGrid.Rows.Add(x.Customer,x.Title,x.Stage,x.Amount);
            followGrid.Rows.Clear(); foreach(var x in data.FollowUps) followGrid.Rows.Add(x.Customer,x.Note,x.Date);
            stats.Text = $"مشتری: {data.Customers.Count}   پروژه: {data.Projects.Count}   پیگیری: {data.FollowUps.Count}";
        }

        void LoadData()
        {
            try { Directory.CreateDirectory(dataDir); if(File.Exists(dataFile)) data = new JavaScriptSerializer().Deserialize<AppData>(File.ReadAllText(dataFile)); }
            catch { }
            if(data==null) data=new AppData();
        }
        void SaveData()
        {
            try { Directory.CreateDirectory(dataDir); File.WriteAllText(dataFile,new JavaScriptSerializer().Serialize(data)); }
            catch(Exception ex){ MessageBox.Show("خطا در ذخیره اطلاعات: "+ex.Message); }
        }
    }
}
