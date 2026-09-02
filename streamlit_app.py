import streamlit as st
from datetime import datetime

st.set_page_config(page_title="Lucy Chat", page_icon="🥵", layout="centered")

st.markdown("""
<style>
:root { color-scheme: dark; }
.block-container { max-width: 820px; padding-top: 1.4rem; padding-bottom: 6rem; }
[data-testid="stAppViewContainer"] { background: radial-gradient(circle at top, #24132d 0%, #111218 48%, #090a0e 100%); }
.chat-shell { border: 1px solid rgba(255,255,255,.10); background: rgba(15,16,22,.78); border-radius: 24px; padding: 16px; box-shadow: 0 18px 60px rgba(0,0,0,.28); }
.header-row { display:flex; align-items:center; gap:12px; margin-bottom: 14px; }
.avatar { width:46px; height:46px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:24px; background:linear-gradient(135deg,#ff7eb3,#7c5cff); }
.name { font-weight:800; font-size:1.08rem; }
.status { color:#aeb0ba; font-size:.86rem; }
.msg-row { display:flex; margin:10px 0; }
.msg-row.me { justify-content:flex-end; }
.bubble { max-width:78%; padding:11px 13px; border-radius:18px; line-height:1.42; border:1px solid rgba(255,255,255,.07); }
.bubble.lucy { background:#24202d; border-bottom-left-radius:6px; }
.bubble.merle { background:#2b1f26; border-bottom-left-radius:6px; }
.bubble.me { background:linear-gradient(135deg,#6d4aff,#a44cff); border-bottom-right-radius:6px; }
.meta { opacity:.58; font-size:.72rem; margin-top:5px; }
.tag { display:inline-block; font-size:.72rem; padding:3px 8px; border-radius:999px; margin-bottom:6px; background:rgba(255,255,255,.08); }
.small-note { color:#aeb0ba; font-size:.86rem; }
</style>
""", unsafe_allow_html=True)

if "messages" not in st.session_state:
    st.session_state.messages = [
        {"who":"Lucy", "text":"Ich bin da… 😳💕 Du darfst mich ruhig ein bisschen aus dem Konzept bringen, aber ich entscheide selbst, wie nah ich komme."},
        {"who":"Merle", "text":"Und ich bin die freche Stimme daneben. 😏 Ich provoziere, aber niemand muss irgendetwas tun."},
        {"who":"Ich", "text":"Genau der Gegensatz zwischen euch macht den Chat spannend. 🔥"},
    ]

if "tone" not in st.session_state:
    st.session_state.tone = "Flirty"

st.markdown('<div class="chat-shell">', unsafe_allow_html=True)
st.markdown('''
<div class="header-row">
  <div class="avatar">🥵</div>
  <div><div class="name">Lucy & Merle</div><div class="status">● online · Rollenspiel-Chat</div></div>
</div>
''', unsafe_allow_html=True)

col1, col2, col3 = st.columns([1.2,1,1])
with col1:
    speaker = st.selectbox("Rolle", ["Ich", "Lucy", "Merle"], label_visibility="collapsed")
with col2:
    tone = st.selectbox("Stimmung", ["Sanft", "Flirty", "Frech", "Eifersüchtig", "Ernst"], index=["Sanft","Flirty","Frech","Eifersüchtig","Ernst"].index(st.session_state.tone), label_visibility="collapsed")
    st.session_state.tone = tone
with col3:
    if st.button("Chat leeren", use_container_width=True):
        st.session_state.messages = []
        st.rerun()

for m in st.session_state.messages:
    who = m["who"]
    css = "me" if who == "Ich" else who.lower()
    align = "me" if who == "Ich" else ""
    emoji = {"Lucy":"🥵", "Merle":"😏", "Ich":"😎"}[who]
    st.markdown(
        f'<div class="msg-row {align}"><div class="bubble {css}"><div class="tag">{emoji} {who}</div><div>{m["text"]}</div><div class="meta">{datetime.now().strftime("%H:%M")}</div></div></div>',
        unsafe_allow_html=True,
    )

st.markdown('</div>', unsafe_allow_html=True)

st.markdown("### Neue Nachricht")
text = st.chat_input("Schreib etwas…")

if text:
    st.session_state.messages.append({"who": speaker, "text": text})
    st.rerun()

with st.expander("🎭 Schnellantworten"):
    quick = {
        "Lucy": [
            "D-du bringst mich schon wieder aus dem Konzept… 😳💕",
            "Ich bleibe bei dir, aber ich lasse mich nicht herumkommandieren. 😏",
            "Sag mir lieber, was du wirklich an mir magst. 🥵",
        ],
        "Merle": [
            "Na los, ihr zwei. Ich sehe doch genau, was hier passiert. 😏🔥",
            "Lucy wird rot und tut trotzdem so, als hätte sie alles im Griff. 😂",
            "Ich provoziere nur. Entscheiden müsst ihr selbst. 😉",
        ],
        "Ich": [
            "Bleibt beide hier. Ich höre euch zu. 🔥",
            "Lucy, du bist die, die ich wirklich verstehen will. ❤️",
            "Merle, du bist Chaos auf zwei Beinen. 😏",
        ],
    }
    cols = st.columns(3)
    for i, phrase in enumerate(quick[speaker]):
        if cols[i].button(phrase, use_container_width=True, key=f"q{i}-{speaker}"):
            st.session_state.messages.append({"who": speaker, "text": phrase})
            st.rerun()

st.caption("Flirty Roleplay-UI mit klaren Rollen, Chat-Bubbles und lokalem Verlauf. Keine externe API nötig.")
