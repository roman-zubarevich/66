<script setup>
</script>

<template>
  <img src="/info-circle.svg" width="24" height="24" class="info" data-bs-toggle="modal" data-bs-target="#rulesModal">

  <div class="modal fade" id="rulesModal" data-bs-keyboard="true" tabindex="-1">
    <div class="modal-dialog modal-dialog-scrollable modal-xl">
      <div class="modal-content">
        <div class="modal-header">
          <h1 class="modal-title fs-4">The 66 game</h1>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <h5>Rules</h5>
          <h6>Overview</h6>
            <p>The deck consists of 52 cards with numeric values: (1 .. 12) x 4 and (0, 13) x 2.<br>From 2 to 5 people can play.</p>
            <p>The goal is to finish with fewer total points than your opponents.<br>The game ends when any player's total points exceed 66.</p>
          <h6>Game round</h6>
            <p>Each player starts with 4 random cards taken from the deck and secretly looks into two of them.</p>
            <p>Players make their moves one by one clockwise, starting from the previous round's winner, if any.</p>
            <p>Instead of making a move, any player can request to stop the round. In this case, the round ends when every other player has made one more move.<br>Otherwise, the round ends when the deck is depleted.</p>
          <h6>Making a move</h6>
            <p>The current player takes a random card from the deck, secretly looking into it, or takes a publicly known card that was recently discarded. He/she then performs one of the following actions:
              <ul>
                <li>Discards any one of his/her own cards, replacing it with the taken card.</li>
                <li>Pretends that 2 or more of his/her own cards are equal. The player exposes these cards to other players but doesn't look into them:</li>
                <ul>
                  <li>If all shown cards are indeed equal, the player discards them, replacing them with the taken card.</li>
                  <li>If any shown cards differ, they remain in their places, and the taken card is discarded.</li>
                </ul>
                <li>Discards the card if it was taken from the deck. If the card appears to have one of the special values, the player performs an additional action:</li>
                <ul>
                  <li>7 or 8: secretly looks into one of his/her own cards.</li>
                  <li>9 or 10: secretly looks into one of any other player's cards.</li>
                  <li>11 or 12: exchanges one of his/her own cards with one of any other player's cards without looking into any of them.</li>
                </ul>
              </ul>
            </p>
          <h6>Score calculation</h6>
            <p>At the end of each round, each player's points are calculated as the sum of his/her own cards.<br>If any player has requested to stop the round and doesn't have the unique lowest points among all players, his/her points are increased by 5 as a penalty.</p>
            <p>The player with the lowest points is considered the round's winner; his/her points are reset to 0.</p>
            <p>After round points have been calculated, they are added to the total points of the respective players.<br>If any player has exactly 66 points in total, the number is halved, leaving the player with 33.</p>
          <h5>Visuals</h5>
          <h6>Interaction</h6>
            <p>When you look at some card secretly, i.e. without exposing it to other players, that card is brought visually closer to you.</p>
            <p>All clickable objects on the table are easily recognizable by their green color. Selectable cards pulse with darker and brighter green, and available action buttons are drawn with green lines.</p>
            <p>Hints are shown in orange color; they are animated but not clickable.</p>
            <p>When a round stop is requested, a stop sign is raised near the player who requested it and remains there until the end of the round.</p>
          <h6>Avatars</h6>
            <p>Every player has a cat avatar. Its main purpose is to visualize certain game events:
              <ul>
                <li>Sleep while waiting for other players to make a move; this helps to track whose turn it is.</li>
                <li>Get close to any cards exposed to the corresponding player and look at them; it allows you to see who gets what information.</li>
                <li>Get crowned if the player has won the game.</li>
              </ul>
            </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info {
  position: fixed;
  top: 20px;
  right: 30px;
  cursor: pointer;
}
</style>