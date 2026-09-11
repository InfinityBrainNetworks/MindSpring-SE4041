# -*- coding: utf-8 -*-
import os
from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.opc.constants import RELATIONSHIP_TYPE as RT

BASE = os.path.dirname(os.path.abspath(__file__))
DESIGN = os.path.join(os.path.dirname(BASE), "design")
TEAL   = "10514C"
AMBER  = "E8A33D"
OFFWHT = "F4F6F1"
NEUTRL = "1B2422"


# ---------- colour maths ----------
def _lum(h):
    h = h.lstrip('#')
    ch = [int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4)]
    f = lambda c: c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = [f(c) for c in ch]
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a, b):
    l1, l2 = _lum(a), _lum(b)
    if l1 < l2:
        l1, l2 = l2, l1
    return (l1 + 0.05) / (l2 + 0.05)


def rating(r, large=False):
    if large:
        return "Pass AAA" if r >= 4.5 else ("Pass AA" if r >= 3.0 else "Fail")
    return "Pass AAA" if r >= 7.0 else ("Pass AA" if r >= 4.5 else "Fail")


# ---------- docx helpers ----------
def shade(cell, hexcolor):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), hexcolor)
    tcPr.append(shd)


def no_borders(table_):
    """Explicitly clear all borders.

    w:tblPr is an ordered sequence, so w:tblBorders must be inserted before its
    schema successors -- appending it would produce a file Word refuses to open.
    """
    tblPr = table_._tbl.tblPr
    borders = OxmlElement('w:tblBorders')
    for edge in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'):
        e = OxmlElement('w:' + edge)
        e.set(qn('w:val'), 'none')
        e.set(qn('w:sz'), '0')
        borders.append(e)
    tblPr.insert_element_before(
        borders, 'w:shd', 'w:tblLayout', 'w:tblCellMar', 'w:tblLook',
        'w:tblCaption', 'w:tblDescription')


def add_hyperlink(paragraph, url, text, size=10.5):
    """Insert a real clickable external hyperlink.

    w:rPr is an ordered sequence -- w:color, then w:sz, then w:u. Emitting these
    out of order produces a file Word refuses to open.
    """
    r_id = paragraph.part.relate_to(url, RT.HYPERLINK, is_external=True)
    hl = OxmlElement('w:hyperlink')
    hl.set(qn('r:id'), r_id)
    run = OxmlElement('w:r')
    rPr = OxmlElement('w:rPr')
    col = OxmlElement('w:color')
    col.set(qn('w:val'), TEAL)
    rPr.append(col)
    sz = OxmlElement('w:sz')
    sz.set(qn('w:val'), str(int(size * 2)))
    rPr.append(sz)
    u = OxmlElement('w:u')
    u.set(qn('w:val'), 'single')
    rPr.append(u)
    run.append(rPr)
    t_ = OxmlElement('w:t')
    t_.text = text
    t_.set(qn('xml:space'), 'preserve')
    run.append(t_)
    hl.append(run)
    paragraph._p.append(hl)
    return hl


def set_updatefields(d):
    """Ask Word to refresh fields (the TOC) when the document is opened.

    w:updateFields must precede w:compat in the CT_Settings sequence.
    """
    s = d.settings.element
    uf = OxmlElement('w:updateFields')
    uf.set(qn('w:val'), 'true')
    s.insert_element_before(
        uf, 'w:compat', 'w:docVars', 'w:rsids', 'm:mathPr', 'w:themeFontLang',
        'w:clrSchemeMapping', 'w:shapeDefaults', 'w:decimalSymbol', 'w:listSeparator')


doc = Document()

# ---------- page & base styles ----------
for sec in doc.sections:
    sec.top_margin = Inches(0.9)
    sec.bottom_margin = Inches(0.9)
    sec.left_margin = Inches(0.9)
    sec.right_margin = Inches(0.9)

st = doc.styles['Normal']
st.font.name = 'Calibri'
st.font.size = Pt(11)
st.element.rPr.rFonts.set(qn('w:eastAsia'), 'Calibri')
st.paragraph_format.space_after = Pt(8)
st.paragraph_format.line_spacing = 1.15

for name, size, color, bold in (('Title', 30, TEAL, True),
                                ('Heading 1', 18, TEAL, True),
                                ('Heading 2', 14, TEAL, True),
                                ('Heading 3', 12, NEUTRL, True)):
    s = doc.styles[name]
    s.font.name = 'Calibri'
    s.font.size = Pt(size)
    s.font.bold = bold
    s.font.color.rgb = RGBColor.from_string(color)
    s.paragraph_format.space_before = Pt(14 if 'Heading' in name else 0)
    s.paragraph_format.space_after = Pt(6)
    try:
        s.element.rPr.rFonts.set(qn('w:eastAsia'), 'Calibri')
    except Exception:
        pass


def P(text="", style=None, align=None, size=None, bold=None, italic=None,
      color=None, space_after=None):
    p = doc.add_paragraph(style=style)
    r = p.add_run(text)
    if size:
        r.font.size = Pt(size)
    if bold is not None:
        r.font.bold = bold
    if italic is not None:
        r.font.italic = italic
    if color:
        r.font.color.rgb = RGBColor.from_string(color)
    if align is not None:
        p.alignment = align
    if space_after is not None:
        p.paragraph_format.space_after = Pt(space_after)
    return p


def B(text):
    p = doc.add_paragraph(text, style='List Bullet')
    p.paragraph_format.space_after = Pt(3)
    return p


FIG = [0]


def figure_row(items, width=2.55):
    """items: list of (path relative to design/, caption). Renders up to 2 per row."""
    for i in range(0, len(items), 2):
        chunk = items[i:i + 2]
        t = doc.add_table(rows=2, cols=len(chunk))
        t.alignment = WD_TABLE_ALIGNMENT.CENTER
        no_borders(t)
        for j, (path, cap) in enumerate(chunk):
            FIG[0] += 1
            c = t.cell(0, j)
            c.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
            c.paragraphs[0].add_run().add_picture(
                os.path.join(DESIGN, path), width=Inches(width))
            cc = t.cell(1, j).paragraphs[0]
            cc.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r = cc.add_run("Figure %d: %s" % (FIG[0], cap))
            r.font.size = Pt(9)
            r.font.italic = True
            r.font.color.rgb = RGBColor.from_string('444444')
            cc.paragraph_format.space_after = Pt(14)
        # Word fuses tables that touch with no paragraph between them, which would
        # merge a 2-column figure row into the 1-column row that follows it.
        spacer = doc.add_paragraph()
        spacer.paragraph_format.space_after = Pt(0)
        spacer.add_run().font.size = Pt(4)


def table(headers, rows, widths=None, header_fill=TEAL):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = 'Table Grid'
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, h in enumerate(headers):
        cell = t.rows[0].cells[i]
        cell.text = ""
        r = cell.paragraphs[0].add_run(h)
        r.font.bold = True
        r.font.size = Pt(10)
        r.font.color.rgb = RGBColor.from_string('FFFFFF')
        shade(cell, header_fill)
    for row in rows:
        cells = t.add_row().cells
        for i, val in enumerate(row):
            cells[i].text = ""
            r = cells[i].paragraphs[0].add_run(str(val))
            r.font.size = Pt(10)
    if widths:
        for r_ in t.rows:
            for i, w in enumerate(widths):
                r_.cells[i].width = Inches(w)
    doc.add_paragraph().paragraph_format.space_after = Pt(4)
    return t


# =====================================================================
# COVER
# =====================================================================
doc.add_paragraph().paragraph_format.space_after = Pt(24)
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.add_run().add_picture(
    os.path.join(DESIGN, "branding", "Logo.png"),
    width=Inches(3.0))

P("MindSpring", style='Title', align=WD_ALIGN_PARAGRAPH.CENTER)
P("Smart Mental Wellness and Habit Tracking Mobile Application",
  align=WD_ALIGN_PARAGRAPH.CENTER, size=14, color=NEUTRL, space_after=2)
P("UI Prototype Design Report (Figma)", align=WD_ALIGN_PARAGRAPH.CENTER,
  size=13, bold=True, color=AMBER, space_after=24)

t = doc.add_table(rows=0, cols=2)
t.alignment = WD_TABLE_ALIGNMENT.CENTER
no_borders(t)
meta = [
    ("Student Name", "Asan M A M"),
    ("IT Number", "IT22253262"),
    ("Module", "SE4041 – Mobile Application Design and Development (MADD)"),
    ("Assignment", "Assignment 01 – Kotlin | Design UI Prototype using Figma (10 Marks)"),
    ("Degree Programme", "BSc (Hons) in Information Technology – Faculty of Computing"),
    ("Academic Year", "Year 4, Semester 1 & 2"),
    ("Allocated Topic", "Smart Mental Wellness and Habit Tracking Mobile Application "
                        "(allocated by last digit “2” of IT22253262)"),
    ("Figma Prototype Link",
     "https://www.figma.com/design/5oKWqb4kQKEbfwo2WO4TXU/MindSprint?node-id=0-1"
     "&t=h66pIejgaQtmURtw-1"),
    ("GitHub Repository", "[ Insert your public repository link ]"),
    ("Date of Submission", "22 August 2026"),
]
for k, v in meta:
    cells = t.add_row().cells
    cells[0].width = Inches(1.9)
    cells[1].width = Inches(4.6)
    r = cells[0].paragraphs[0].add_run(k)
    r.font.bold = True
    r.font.size = Pt(10.5)
    r.font.color.rgb = RGBColor.from_string(TEAL)
    if v.startswith("http"):
        add_hyperlink(cells[1].paragraphs[0], v, v, size=9)
    else:
        r2 = cells[1].paragraphs[0].add_run(v)
        r2.font.size = Pt(10.5)

doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

# =====================================================================
# TOC
# =====================================================================
P("Table of Contents", style='Heading 1')
tp = doc.add_paragraph()
fld = OxmlElement('w:fldSimple')
fld.set(qn('w:instr'), 'TOC \\o "1-2" \\h \\z \\u')
inner = OxmlElement('w:r')
it = OxmlElement('w:t')
it.text = "Right-click here and choose 'Update Field' to generate the table of contents."
inner.append(it)
fld.append(inner)
# w:fldSimple is run-level content: it must be a child of the w:p, not a sibling.
tp._p.append(fld)
doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

# =====================================================================
# 1. OVERVIEW
# =====================================================================
P("1. Overview of the Application", style='Heading 1')
P("MindSpring is a mobile application for Android that helps people look after their day-to-day mental "
  "wellbeing by joining together two behaviours that are usually kept apart: tracking the small habits that "
  "shape a person's routine, and recording how that person actually feels. Most habit trackers count streaks "
  "but never ask why a streak broke, and most mood journals record feelings but never connect them to "
  "behaviour. MindSpring closes that loop. It logs habits and moods in the same place, and then reflects the "
  "relationship between them back to the user as a plain-language insight, such as “You rate your mood 32% "
  "higher on days you exercise.”")
P("This report covers Phase 01 of the assignment: the complete user-interface prototype designed in Figma "
  "before any Kotlin development begins. It documents the ideation behind the product, the application of the "
  "60-30-10 colour rule, the design of the main interfaces, and the aesthetic and usability decisions that "
  "underpin the prototype. Every screen shown in this report is taken directly from the Figma prototype file.")

P("1.1 Product Identity", style='Heading 2')
P("The name MindSpring carries two deliberate readings. A spring is a natural source of fresh water — "
  "steady, self-renewing, quietly persistent — which mirrors how small daily habits accumulate into "
  "wellbeing. Spring is also the season of growth after dormancy, which speaks to recovery and progress. The "
  "logo reinforces this: the descender of the letter “g” is redrawn as a growing stem with two leaves, "
  "so the mark is literally a word that sprouts. The wordmark is split across the two brand colours — "
  "“Mind” in deep teal for calm and stability, “Spring” in amber for warmth and energy — "
  "which introduces the colour system on the very first screen the user sees.")

P("1.2 Design Principles", style='Heading 2')
P("Four principles governed every decision in the prototype:")
B("Calm over competitive. Wellness software must not create the anxiety it claims to relieve. Streaks are "
  "encouraged, never punished; there are no red “failure” states, no leaderboards, and no guilt-driven "
  "notification copy.")
B("Low interaction cost. A mood check-in must be completable in under ten seconds, because a reflection tool "
  "that feels like paperwork will be abandoned within a week.")
B("Privacy by default. The application is offline-first. This is stated plainly during onboarding "
  "(“Your Private Sanctuary”) and again at registration (“All your data stays on this device”), "
  "because trust is a precondition for honest self-reporting.")
B("Honest scope. MindSpring is a self-reflection tool, not a clinical product. A permanent disclaimer on the "
  "Profile screen states that it is not medical advice.")

doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

# =====================================================================
# 2. IDEATION
# =====================================================================
P("2. Ideation", style='Heading 1')

P("2.1 Problem Identification", style='Heading 2')
P("The topic allocated by the last digit of registration number IT22253262 is mental wellness and habit "
  "tracking. Examining the existing product category revealed four recurring problems that the prototype sets "
  "out to solve:")
table(["#", "Problem Observed", "Consequence for the User", "How MindSpring Responds"],
      [["1", "Habit trackers and mood journals exist as separate applications.",
        "The user records behaviour and feeling in two places and never sees how one drives the other.",
        "A single application logs both, and the Insights screen correlates them."],
       ["2", "Streak-based trackers punish a missed day with red markers and lost progress.",
        "Breaking a streak triggers guilt and app abandonment — the opposite of a wellness outcome.",
        "Progress is shown as proportion complete and personal best; a missed day is simply an unfilled dot."],
       ["3", "Journalling apps present a blank page and expect free writing.",
        "The blank page is intimidating; most users write nothing and stop returning.",
        "Mood check-in uses a five-point face scale plus pre-set feeling chips; the note field is optional."],
       ["4", "Wellness apps commonly upload personal reflections to cloud servers.",
        "Users self-censor, so the recorded data is less honest and therefore less useful.",
        "Offline-first local storage, declared explicitly at onboarding and registration."]],
      widths=[0.35, 1.7, 2.2, 2.35])

P("2.2 Target Audience", style='Heading 2')
P("The primary audience is university students and early-career working adults aged roughly 18 to 35 — a "
  "group with high smartphone fluency, irregular schedules, and well-documented exposure to academic and "
  "workplace stress. Two representative user profiles guided the design:")
table(["Profile", "Context", "Goal", "Design Implication"],
      [["Undergraduate student, 22",
        "Irregular timetable, deadline-driven stress, studies late at night.",
        "Build consistent study and sleep habits and understand what wrecks a productive day.",
        "Fast one-tap habit completion; a wind-down cue in the evening; per-habit reminder times."],
       ["Junior software engineer, 28",
        "Sedentary desk work, long screen hours, low-grade daily stress.",
        "Take short recovery breaks and notice stress patterns before they accumulate.",
        "A dedicated Calm tab with a timed breathing exercise reachable in two taps from any screen."]],
      widths=[1.3, 1.75, 1.75, 1.8])
P("A secondary audience is any adult beginning a self-improvement routine who wants a private, low-pressure "
  "record of progress. Users in acute clinical distress are explicitly out of scope, and the application "
  "states this rather than implying therapeutic capability.")

P("2.3 Feasibility Study", style='Heading 2')
table(["Dimension", "Assessment"],
      [["Technical",
        "Every feature maps to standard Android capability. Kotlin with Jetpack Compose or XML layouts covers "
        "the interface; Room over SQLite provides local persistence; WorkManager and AlarmManager schedule "
        "habit reminders; MPAndroidChart or Compose Canvas draws the trend and bar charts. No server, no "
        "third-party API and no sensor hardware is required, which removes the largest sources of technical risk."],
       ["Economic",
        "Development cost is limited to student time. Android Studio, Figma (education plan) and Room are free. "
        "Because the application is offline-first there are no hosting, database or bandwidth costs, so the "
        "product is viable at zero recurring expense."],
       ["Operational",
        "The interaction model — tap a checkbox, tap a face, read a chart — matches patterns users "
        "already know from messaging and fitness apps, so the learning curve is minimal. Daily use costs under "
        "a minute."],
       ["Schedule",
        "The scope is deliberately sized to one semester: five primary destinations and eighteen designed "
        "frames, with the prototype fully specified before development so implementation is translation "
        "rather than exploration."],
       ["Legal and Ethical",
        "Storing mood data locally avoids the data-protection obligations that cloud storage would introduce. "
        "The non-medical disclaimer and the Clear All Data control address the ethical duty of a wellness "
        "product to be honest about its limits and to let users erase sensitive records."]],
      widths=[1.35, 5.25])

P("2.4 Scope of the Application", style='Heading 2')
P("Within scope:", bold=True, space_after=2)
B("Account creation and local sign-in, with a three-step onboarding sequence.")
B("Habit management — create, categorise, assign an icon, set a weekly frequency and a reminder time, "
  "complete, view detail and delete.")
B("Daily mood check-in on a five-point scale with multi-select feeling tags and an optional note.")
B("A browsable and filterable mood history with swipe-to-delete.")
B("A Calm section containing a guided breathing exercise with selectable session lengths, and a gratitude "
  "journal with past entries.")
B("An Insights section presenting mood trends, habit-completion rates, streaks and a generated key insight, "
  "filterable by week, month or year.")
B("Profile and settings — edit profile, manage reminders, toggle dark mode, export data and clear all data.")
P("Outside scope:", bold=True, space_after=2)
B("Cloud synchronisation, multi-device accounts and social or community features.")
B("Clinical assessment, diagnosis, therapy content or crisis intervention.")
B("Wearable and health-platform integration, and any form of advertising or in-app purchase.")

P("2.5 Feature-to-Screen Traceability", style='Heading 2')
P("Each defined feature was mapped to a concrete screen before visual design began, which ensured the "
  "prototype covers the requirement set rather than an arbitrary selection of attractive screens.")
table(["Feature", "Prototype Screen(s)", "Primary Navigation Path"],
      [["First-run explanation and value proposition", "Onboarding (3 slides)", "App launch (first run only)"],
       ["Account creation and authentication", "Register, Login", "App launch"],
       ["Daily overview and quick actions", "Home Dashboard (light and dark)", "Home tab"],
       ["Habit tracking and management", "Habits List, Add Habit, Habit Detail", "Habits tab"],
       ["Positive reinforcement on completion", "Habit Completed", "Habits tab → tap complete"],
       ["Mood logging", "Mood Check-in", "Home → Check in"],
       ["Mood review and editing", "Mood History", "Insights tab → History"],
       ["Guided relaxation", "Breathing Exercise (light and dark)", "Calm tab"],
       ["Reflective journalling", "Gratitude Journal", "Calm tab"],
       ["Analytics and correlation", "Insights (light and dark)", "Insights tab"],
       ["Personalisation and data control", "Profile and Settings", "Profile tab"]],
      widths=[2.3, 2.3, 2.0])

doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

# =====================================================================
# 3. 60-30-10
# =====================================================================
P("3. Application of the 60-30-10 Colour Rule", style='Heading 1')
P("The 60-30-10 rule is a proportioning principle borrowed from interior design: roughly 60% of a composition "
  "is given to a dominant base colour, 30% to a supporting colour that provides structure and contrast, and "
  "10% to an accent reserved for the elements that matter most. Applied to an interface, it prevents the two "
  "most common colour failures — a flat screen with no visual hierarchy, and a loud screen where every "
  "element competes for attention. In MindSpring the accent is treated as a scarce resource: amber marks the "
  "single most important action on any given screen, and nothing else.")

P("3.1 The Colour Palette", style='Heading 2')
P("A four-colour system was defined in Figma as reusable colour styles, each with a tonal ramp so that hover, "
  "pressed, disabled and surface variants derive from the same hue rather than being chosen ad hoc.")

pal = [("Primary — Deep Teal", "#" + TEAL, "FFFFFF", TEAL,
        "Structure and calm. App bars, bottom navigation, hero cards, headings, primary icons and filled buttons."),
       ("Secondary — Amber", "#" + AMBER, "1B2422", AMBER,
        "Energy and attention. Primary call-to-action buttons, streak flames, active navigation item, key data points."),
       ("Tertiary — Off-White", "#" + OFFWHT, "1B2422", OFFWHT,
        "The canvas. Screen backgrounds and card surfaces in light mode; a soft off-white chosen over pure white to reduce glare."),
       ("Neutral — Near-Black", "#" + NEUTRL, "FFFFFF", NEUTRL,
        "Legibility and depth. Body text in light mode and the base surface of dark mode.")]
t = doc.add_table(rows=1, cols=3)
t.style = 'Table Grid'
for i, h in enumerate(["Swatch", "Role and Hex", "Where It Is Used"]):
    c = t.rows[0].cells[i]
    c.text = ""
    r = c.paragraphs[0].add_run(h)
    r.font.bold = True
    r.font.size = Pt(10)
    r.font.color.rgb = RGBColor.from_string('FFFFFF')
    shade(c, TEAL)
for name, hexv, fg, fill, use in pal:
    cells = t.add_row().cells
    cells[0].width = Inches(0.95)
    cells[1].width = Inches(2.05)
    cells[2].width = Inches(3.6)
    shade(cells[0], fill)
    pr = cells[0].paragraphs[0]
    pr.alignment = WD_ALIGN_PARAGRAPH.CENTER
    rr = pr.add_run(hexv)
    rr.font.size = Pt(9)
    rr.font.bold = True
    rr.font.color.rgb = RGBColor.from_string(fg)
    p1 = cells[1].paragraphs[0]
    r1 = p1.add_run(name)
    r1.font.bold = True
    r1.font.size = Pt(10)
    r1.font.color.rgb = RGBColor.from_string(TEAL)
    p1.add_run("\n" + hexv).font.size = Pt(9)
    r3 = cells[2].paragraphs[0].add_run(use)
    r3.font.size = Pt(10)
doc.add_paragraph().paragraph_format.space_after = Pt(6)

figure_row([("branding/Design System.png",
             "The MindSpring design system in Figma — colour styles with tonal ramps, the Plus Jakarta Sans "
             "type scale, and the shared button, input, navigation and icon components.")],
           width=6.3)

P("3.2 Proportional Allocation", style='Heading 2')
table(["Share", "Role", "Colour Applied", "Interface Elements"],
      [["≈60%", "Dominant / base", "Tertiary Off-White #" + OFFWHT,
        "Screen backgrounds, card and list surfaces, input fields, unselected chips. Large uninterrupted areas "
        "of quiet colour give the eye somewhere to rest — essential for a calm-first product."],
       ["≈30%", "Supporting / structural", "Primary Deep Teal #" + TEAL,
        "Top app bars, the bottom navigation bar, hero and streak cards, section headings, completed-habit "
        "checkmarks, outline icons and secondary buttons. Teal builds the skeleton of every screen and "
        "separates chrome from content."],
       ["≈10%", "Accent", "Secondary Amber #" + AMBER,
        "Exactly one primary action per screen — Get Started, Log In, Check in, Save Entry, the floating "
        "add-habit button — plus streak flames, the active navigation label, and the highlighted data "
        "point in a chart."]],
      widths=[0.65, 1.4, 1.85, 2.7])
P("Because amber appears only where the user is meant to act, the interface teaches its own affordance: after "
  "two or three screens a user learns that amber means “this is what you do next”, and the primary "
  "action on an unfamiliar screen becomes findable without reading. The near-black neutral is used for body "
  "copy rather than counted as one of the three proportions, since it functions as ink rather than as colour.")

P("3.3 The Rule in Light Mode", style='Heading 2')
P("The Home Dashboard is the clearest demonstration. The off-white canvas and the white habit and streak cards "
  "occupy the majority of the screen. The teal app bar, the teal mood hero card and the teal bottom navigation "
  "form the supporting band of structure, roughly a third of the composition. Amber appears exactly twice: on "
  "the Check in button and on the active Home navigation icon. The Habits List repeats the pattern — an "
  "off-white field of white cards, a teal app bar and navigation, and amber reserved for the floating action "
  "button and for the one habit that has reached a full seven-of-seven week.")

P("3.4 The Rule in Dark Mode", style='Heading 2')
P("Dark mode does not invert the palette; it re-assigns the roles while preserving the same 60-30-10 "
  "proportions, which is what keeps the two themes recognisable as one product:")
table(["Share", "Light Mode", "Dark Mode"],
      [["≈60%", "Tertiary Off-White #" + OFFWHT + " backgrounds",
        "Neutral Near-Black #" + NEUTRL + " backgrounds, with cards raised one step lighter to signal elevation"],
       ["≈30%", "Primary Deep Teal #" + TEAL + " chrome and hero surfaces",
        "Desaturated teal for app bars, streak cards and the breathing halo, dimmed to avoid glare in low light"],
       ["≈10%", "Amber #" + AMBER + " actions and highlights",
        "Amber retained at full strength — it reads even more distinctly against near-black, so accents "
        "stay unambiguous"]],
      widths=[0.7, 2.9, 3.0])
P("The dark Insights screen illustrates the discipline of the accent: the weekly activity chart draws six bars "
  "in teal and only the current day in amber, so the eye lands on today without a label being needed.")

P("3.5 Contrast Verification", style='Heading 2')
P("Colour proportion is only defensible if the resulting combinations are legible. Every pairing used in the "
  "prototype was measured against the WCAG 2.1 contrast formula. The ratios below are computed directly from "
  "the palette hex values.")
pairs = [("Deep Teal text on Off-White", TEAL, OFFWHT, False, "Headings, links, section titles"),
         ("Near-Black body text on Off-White", NEUTRL, OFFWHT, False, "All body copy in light mode"),
         ("White text on Deep Teal", "FFFFFF", TEAL, False, "App bar titles, hero card text"),
         ("Near-Black label on Amber button", NEUTRL, AMBER, False, "Primary call-to-action labels"),
         ("Amber on Deep Teal", AMBER, TEAL, True, "Large streak figures on hero cards"),
         ("Amber on Near-Black (dark mode)", AMBER, NEUTRL, False, "Accents and active nav in dark mode"),
         ("Off-White text on Near-Black", OFFWHT, NEUTRL, False, "Body copy in dark mode")]
rows = []
for label, fg, bg, large, use in pairs:
    ratio = contrast(fg, bg)
    rows.append([label, "%.2f : 1" % ratio,
                 rating(ratio, large) + (" (large text)" if large else ""), use])
table(["Foreground on Background", "Ratio", "WCAG 2.1 Result", "Applied To"], rows,
      widths=[2.2, 0.85, 1.5, 2.05])
P("Two decisions follow directly from these measurements. First, amber is never used for small text on the "
  "off-white canvas, because its luminance is too close to the background; it is used as a filled surface with "
  "near-black text on top, which is a strong pairing. Second, amber on deep teal is restricted to large "
  "numerals such as the “12 Day Streak” figure, where the large-text threshold applies. Colour is also "
  "never the sole carrier of meaning: a completed habit shows a checkmark and struck-through text as well as a "
  "colour change, so the interface remains readable for colour-blind users.")

doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

# =====================================================================
# 4. MAIN INTERFACE DESIGN
# =====================================================================
P("4. Main Interface Design", style='Heading 1')

P("4.1 Information Architecture", style='Heading 2')
P("The application is organised around five persistent destinations exposed in a bottom navigation bar. Five "
  "is the practical maximum for thumb-reachable tabs, and the grouping keeps every core task within two taps "
  "of app launch. Onboarding, authentication and the immersive breathing session are the only screens that "
  "hide the navigation bar, because each demands undivided attention.")
ia = ["Launch → Onboarding (first run) → Register / Login",
      "  Home      → mood prompt, today's habits, current streaks → Mood Check-in",
      "  Habits    → Habits List → Add Habit | Habit Detail | Habit Completed",
      "  Insights  → trends, completion rates, key insight → Mood History",
      "  Calm      → Breathing Exercise | Gratitude Journal",
      "  Profile   → Edit Profile, Reminders, Dark Mode, Export Data, About, Clear All Data"]
for line in ia:
    pp = doc.add_paragraph()
    r = pp.add_run(line)
    r.font.name = 'Consolas'
    r.font.size = Pt(9.5)
    r.font.color.rgb = RGBColor.from_string(TEAL)
    pp.paragraph_format.space_after = Pt(1)
doc.add_paragraph().paragraph_format.space_after = Pt(4)

P("4.2 Layout System", style='Heading 2')
P("All screens were built on an 8-point spacing grid with a 16 dp outer margin, so that padding, gaps and "
  "component heights are always multiples of eight. Content is presented in cards with a 16 dp corner radius "
  "and a soft shadow, which chunks information into scannable units and suits the rounded character of the "
  "brand. Interactive targets are never smaller than 48 dp square. Primary buttons are full-width pills "
  "anchored near the bottom of the screen, inside comfortable thumb reach on a modern phone. Frames were "
  "designed at a 390 × 844 baseline and built with auto-layout so the same components respond correctly on "
  "larger and smaller devices — which also means each Figma frame maps cleanly onto an Android layout "
  "during development.")

P("4.3 Onboarding and Authentication", style='Heading 2')
P("The first slide of onboarding leads with privacy rather than features, because trust is what determines "
  "whether a user will record anything honest. A hand-drawn seedling illustration echoes the logo, a "
  "three-dot indicator sets expectations about length, and a Skip control respects returning users. The Login "
  "and Register screens use the same elevated white card on the off-white canvas, keeping the form visually "
  "self-contained. Register restates the privacy commitment at the point of consent — “All your data "
  "stays on this device” — placing the reassurance exactly where hesitation occurs.")
figure_row([("screens/Onboarding.png", "Onboarding — privacy-first value proposition with page indicator and Skip."),
            ("screens/Login.png", "Login — branded card layout with amber primary action and a clear route to registration."),
            ("screens/Register.png", "Register — account creation with an inline privacy note at the consent checkbox.")])

P("4.4 Home Dashboard", style='Heading 2')
P("Home answers a single question: what does the user need to do today? A time-aware greeting personalises the "
  "screen, the teal hero card asks for a mood check-in with five tappable faces and an amber Check in button, "
  "and Today's Habits shows a progress bar with an explicit “2 of 4 complete” count so progress is "
  "legible at a glance. Habits can be ticked directly from this screen, which removes a navigation step from "
  "the most frequent action in the app. Completed items are struck through and greyed rather than removed, "
  "preserving a visible sense of accomplishment.")
P("The dark variant adapts to context rather than merely recolouring: an Evening Reflection card carries a "
  "quotation and a “Wind down phase active” cue, and the streak count is promoted to its own tile. This "
  "demonstrates that the design system supports contextual variation without breaking its own rules.")
figure_row([("screens/Home Dashboard.png",
             "Home Dashboard (light) — greeting, mood prompt, today's habits with progress, and streaks."),
            ("screens/Home Dashboard (Dark).png",
             "Home Dashboard (dark) — evening reflection, streak tile and habit checklist on a near-black canvas.")])

P("4.5 Habit Tracking", style='Heading 2')
P("The Habits List renders each habit as a card carrying a category icon, a plain-language progress line "
  "(“4 of 7 this week”), a seven-dot week strip and a single large completion button. The dot strip is a "
  "deliberate alternative to a percentage: it shows which days were met without arithmetic. A habit that has "
  "reached seven of seven turns its dots amber and its button to a double-check confirmation state, so success "
  "is visible without a congratulatory interruption. A floating amber action button adds a new habit.")
P("Add Habit breaks configuration into labelled groups — name, category, icon, frequency and reminder — "
  "so the form reads as five small decisions rather than one long list. Category and weekday selectors use "
  "chips and circular toggles instead of dropdowns, which makes the current selection visible without opening "
  "a menu. Habit Detail leads with a streak hero card and then presents completion percentage, personal best, "
  "total days and a thirty-day activity heatmap, giving both an immediate emotional signal and the underlying "
  "evidence. The Habit Completed screen is the app's one moment of celebration: a full-screen checkmark, "
  "sparkle motifs and a personalised message that credits consistency rather than perfection.")
figure_row([("screens/Habits List.png",
             "Habits List — weekly dot progress, one-tap completion and a floating add action."),
            ("screens/Add Habit.png",
             "Add Habit — grouped configuration: name, category, icon, weekday frequency and reminder."),
            ("screens/Habit Detail.png",
             "Habit Detail — streak hero card with completion rate, personal best, total days and activity heatmap."),
            ("screens/Habit Completed!.png",
             "Habit Completed — positive reinforcement with a personalised, non-competitive message.")])

P("4.6 Mood Logging and History", style='Heading 2')
P("Mood Check-in is structured as three progressively optional steps. A five-point face scale answers "
  "“How was your day?” in one tap. Feeling chips — Calm, Anxious, Tired, Motivated, Stressed, "
  "Grateful, Lonely, Focused — allow multiple selections and give vocabulary to users who struggle to name "
  "a feeling. The free-text note is explicitly labelled optional, so a complete entry can be made in a single "
  "tap. This graduated commitment is the main defence against the blank-page problem that causes journalling "
  "apps to be abandoned.")
P("Mood History presents entries as a reverse-chronological card list filtered by week, month or year. Each "
  "card shows a coloured mood dot, a label, a timestamp, the selected feeling tags and an excerpt of the note. "
  "Swiping a card left reveals a red delete action — the only use of red in the entire product, reserved "
  "exclusively for destructive operations, which keeps its meaning unambiguous.")
figure_row([("screens/Mood Check-in.png",
             "Mood Check-in — face scale, multi-select feeling chips and an optional note."),
            ("screens/Mood History.png",
             "Mood History — filterable entry list with swipe-to-delete revealed on the third card.")])

P("4.7 The Calm Section", style='Heading 2')
P("The breathing exercise is the only truly immersive screen in the application. Navigation chrome is removed, "
  "the background becomes a deep teal gradient and the sole controls are a duration selector, a close button "
  "and End session. A central amber orb expands and contracts with concentric rings to pace the breath, "
  "supported by a text instruction (“Breathe in”) and a countdown, so the user can follow the rhythm "
  "visually or by reading. The dark variant presents a named technique — Box Breathing, 4-4-4-4 — with "
  "restart, pause and settings controls, showing how the same immersive frame accommodates a more advanced "
  "session.")
P("The Gratitude Journal replaces the intimidating blank page with a single specific prompt: “Name one "
  "thing that went well today.” The placeholder text continues the sentence for the user, and past entries "
  "appear immediately below with dates and excerpts, so the screen doubles as evidence of accumulated positive "
  "reflection.")
figure_row([("screens/Breathing Exercise.png",
             "Breathing Exercise (light) — immersive gradient, expanding orb, phase instruction and countdown."),
            ("screens/Breathing Exercise (Dark).png",
             "Breathing Exercise (dark) — Box Breathing 4-4-4-4 with restart, pause and settings controls."),
            ("screens/Gratitude Journal.png",
             "Gratitude Journal — a specific prompt, a guided placeholder and a list of past entries.")])

P("4.8 Insights", style='Heading 2')
P("Insights is where MindSpring delivers on its central promise. Rather than opening with a chart, the screen "
  "opens with a plain-sentence key insight in an amber-outlined card — “You rate your mood 32% higher "
  "on days you exercise” — which converts data into something the user can act on without interpreting "
  "a graph. Beneath it, a smooth mood-trend line with amber data points and a dashed baseline shows direction "
  "over time, and habit-completion bars give per-habit percentages. A week / month / year segmented control "
  "sets the period for the whole screen.")
P("The dark variant demonstrates an alternative summary layout: current streak and average mood as large "
  "labelled statistics, followed by a weekly activity bar chart in which only the current day is drawn in "
  "amber. In both variants the numeric value is always accompanied by a short interpretation — “Based "
  "on last 7 days”, “Keep it up! You're on a roll” — so the user is never left to infer whether "
  "a figure is good.")
figure_row([("screens/Insights.png",
             "Insights (light) — key insight card, mood trend line and habit completion rates with a period filter."),
            ("screens/Insights (Dark).png",
             "Insights (dark) — summary statistics and a weekly activity chart highlighting the current day in amber.")])

P("4.9 Profile and Settings", style='Heading 2')
P("Profile groups personalisation and data control into a single scannable list, each row pairing a teal "
  "outline icon with a label and a chevron. Dark Mode is exposed as an inline switch rather than a sub-page, "
  "since it is the setting most likely to be changed. Export Data and Clear All Data give the user real "
  "ownership of their records, which is the practical expression of the privacy promise made at onboarding. "
  "Clear All Data is the one destructive control in settings and is therefore separated into its own card and "
  "rendered in red, deliberately breaking the palette so it cannot be tapped by habit. The non-medical "
  "disclaimer sits permanently at the foot of the screen.")
figure_row([("screens/Profile.png",
             "Profile and Settings — personalisation, dark-mode switch, data export, an isolated destructive "
             "action, and the non-medical disclaimer.")],
           width=2.55)

doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

# =====================================================================
# 5. AESTHETICS AND USABILITY
# =====================================================================
P("5. Design Aesthetics and Usability", style='Heading 1')

P("5.1 Typography", style='Heading 2')
P("A single typeface, Plus Jakarta Sans, is used throughout. It is a geometric sans with generously rounded "
  "counters and slightly humanist letterforms — friendly enough for a wellness product but neutral enough "
  "for dense data screens, which avoids the tonal mismatch that occurs when a decorative display face is "
  "paired with charts. Four roles were defined as Figma text styles and reused everywhere:")
table(["Role", "Weight and Size", "Applied To"],
      [["Headline", "Bold, 24–32 pt", "Screen titles, hero figures, celebration messages"],
       ["Title", "SemiBold, 18–20 pt", "Card headings such as Today's Habits and Mood Trend"],
       ["Body", "Regular, 14–16 pt", "Descriptions, journal entries, list content"],
       ["Label", "Medium, 11–13 pt", "Buttons, chips, navigation labels, metadata and timestamps"]],
      widths=[1.2, 1.7, 3.7])
P("Hierarchy is carried by weight and size rather than by colour, so the type system continues to work in "
  "greyscale and in dark mode. Line length in body copy is held to roughly 40–50 characters, and line "
  "height is set at approximately 1.4 for comfortable reading of longer journal text.")

P("5.2 Shape, Elevation and Spacing", style='Heading 2')
P("A consistent shape language runs through the product: 16 dp radius on cards, fully rounded pill buttons and "
  "chips, and circular icon containers and selectors. Nothing in the interface has a sharp corner, which is a "
  "deliberate reinforcement of the calm positioning — rounded forms read as softer and less clinical. "
  "Elevation is expressed through low-opacity shadows and, in dark mode, through surface lightness rather than "
  "shadow, which is the correct technique for dark themes where shadows are effectively invisible. Generous "
  "whitespace between cards prevents the density that would make a wellness app feel like a dashboard.")

P("5.3 Iconography and Illustration", style='Heading 2')
P("Icons come from a single outline set at a uniform stroke weight, ensuring the navigation bar and habit "
  "categories read as one family. The active navigation item is distinguished by both a filled variant and an "
  "amber tint — two simultaneous signals, so state is never conveyed by colour alone. Habit categories each "
  "carry a distinct icon inside a tinted circle, which makes the list scannable by shape before it is read as "
  "text. Illustration is used sparingly: a hand-drawn seedling at onboarding, expressive faces for mood "
  "selection, and a flame for streaks. Restricting decorative imagery to these moments keeps the working "
  "screens uncluttered.")

P("5.4 Feedback and Motion", style='Heading 2')
P("Every user action produces a visible response. Ticking a habit fills the checkbox in teal, strikes through "
  "the label and advances the progress bar. Reaching a goal opens the celebration screen. Selecting a mood face "
  "fills its container; selecting a feeling chip inverts it. Swiping a mood card reveals a delete action rather "
  "than deleting immediately, so the gesture is discoverable and recoverable. In the breathing session the orb "
  "animates continuously as the pacing mechanism itself, and prototype transitions in Figma use short eased "
  "slides between screens with a longer, gentler ease for the breathing animation — motion is used to "
  "regulate pace, not to entertain.")

P("5.5 Usability Evaluation", style='Heading 2')
P("The prototype was reviewed against Nielsen's usability heuristics. The table records the specific design "
  "response for each, which is how usability was verified before development rather than discovered after it.")
table(["Heuristic", "How the Prototype Satisfies It"],
      [["Visibility of system status",
        "Progress bars with explicit counts, weekly dot strips, completion percentages, a session countdown, "
        "and an active tab that is both filled and amber."],
       ["Match with the real world",
        "Plain language throughout — “How was your day?”, “4 of 7 this week”, "
        "“Keep it burning!” — with familiar metaphors such as a flame for a streak and faces for mood."],
       ["User control and freedom",
        "Back and close controls on every secondary screen, Skip during onboarding, End session during "
        "breathing, swipe-to-delete on history entries, and Clear All Data in settings."],
       ["Consistency and standards",
        "One component library drives all screens; app bars, cards, buttons, chips and the navigation bar "
        "behave identically everywhere and follow Android conventions."],
       ["Error prevention",
        "Confirm-password on registration, an explicit consent checkbox, a reminder toggle that prevents "
        "accidental scheduling, and destructive actions isolated behind a swipe or a separate red card."],
       ["Recognition over recall",
        "Pre-set feeling chips, icon pickers and category chips present options visually; the gratitude prompt "
        "supplies the opening words instead of expecting free recall."],
       ["Flexibility and efficiency",
        "Habits can be completed directly from Home without opening the Habits tab; breathing durations are "
        "preset at 1, 3 and 5 minutes; a period filter is reused across Insights and History."],
       ["Aesthetic and minimalist design",
        "One primary action per screen, optional fields marked as optional, and decoration confined to "
        "onboarding and celebration moments."],
       ["Help users recognise and recover",
        "No punitive error states; a missed habit is simply an unfilled dot, and destructive actions require a "
        "deliberate gesture rather than a single stray tap."],
       ["Help and documentation",
        "Onboarding explains the value proposition, an About entry sits in Profile, and inline helper text "
        "supports the journal and mood screens."]],
      widths=[1.7, 4.9])

P("5.6 Accessibility", style='Heading 2')
B("All text and background pairings meet or exceed the WCAG 2.1 AA threshold, as measured in Section 3.5.")
B("Colour is never the only signal: completion is shown by a checkmark plus strikethrough, the active tab by a "
  "filled icon plus a colour change, and mood entries by a written label beside the coloured dot.")
B("Touch targets are at least 48 dp; mood faces, weekday toggles and completion buttons are deliberately "
  "oversized for one-handed use.")
B("Type is set in scalable units so the layout tolerates the system font-size setting without truncation.")
B("A full dark theme reduces eye strain for evening use, which is when a wellness application is most likely "
  "to be opened.")

P("5.7 Ethical and Emotional Design", style='Heading 2')
P("Two decisions matter more than any visual choice. First, the application never shames the user: there is no "
  "red for a missed habit, no broken-streak alert and no comparison against other people. Second, MindSpring "
  "does not overstate what it is — the Profile screen carries a permanent statement that it is a "
  "self-reflection tool and not medical advice. For an application in the mental-health domain, refusing to "
  "imply clinical authority is a design responsibility, not a legal footnote.")

doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)

# =====================================================================
# 6. CONCLUSION
# =====================================================================
P("6. Conclusion", style='Heading 1')
P("The MindSpring prototype delivers a complete, self-consistent interface for a smart mental wellness and "
  "habit tracking application. The ideation is grounded in four observed failures of the existing product "
  "category, and the response to each is traceable to a specific screen. The 60-30-10 rule is applied through "
  "a formally defined four-colour system — an off-white canvas at roughly 60%, deep teal structure at "
  "roughly 30% and amber accents at roughly 10% — with the same proportions preserved in dark mode and "
  "every pairing verified against WCAG contrast thresholds. The eighteen designed frames are organised around "
  "five thumb-reachable destinations on a consistent 8-point grid, so that no core task is more than two taps "
  "from launch. Aesthetically the product holds a single typeface, one shape language and one icon family "
  "across both themes, and its usability was validated against established heuristics before any code was "
  "written.")
P("Because the prototype was built in Figma from reusable colour, type and component styles, each frame "
  "translates directly into an Android layout, and the design system maps onto a Compose or XML theme with "
  "little reinterpretation. The next phase of the assignment implements these screens in Kotlin with Room for "
  "local persistence, carrying the palette, spacing scale and component behaviour documented here into the "
  "working application.")

P("Appendix A: Screen Index", style='Heading 1')
table(["Figure", "Screen", "Section"],
      [["1", "Design system — colour styles, type scale and components", "3.1"],
       ["2–4", "Onboarding, Login, Register", "4.3"],
       ["5–6", "Home Dashboard — light and dark", "4.4"],
       ["7–10", "Habits List, Add Habit, Habit Detail, Habit Completed", "4.5"],
       ["11–12", "Mood Check-in, Mood History", "4.6"],
       ["13–15", "Breathing Exercise (light and dark), Gratitude Journal", "4.7"],
       ["16–17", "Insights — light and dark", "4.8"],
       ["18", "Profile and Settings", "4.9"]],
      widths=[0.9, 4.5, 1.2])

# ---------- footer with page number ----------
for sec in doc.sections:
    fp = sec.footer.paragraphs[0]
    fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = fp.add_run("MindSpring — UI Prototype Design Report  |  IT22253262  |  Page ")
    r.font.size = Pt(8.5)
    r.font.color.rgb = RGBColor.from_string('777777')
    f = OxmlElement('w:fldSimple')
    f.set(qn('w:instr'), 'PAGE')
    fp._p.append(f)

set_updatefields(doc)
out = os.path.join(BASE, "MindSpring_UI_Prototype_Report_IT22253262.docx")
doc.save(out)
print("Saved:", out)
print("Figures rendered:", FIG[0])
for label, fg, bg, large, use in pairs:
    ratio = contrast(fg, bg)
    print("  %-38s %.2f:1  %s" % (label, ratio, rating(ratio, large)))
